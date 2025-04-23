package ru.testtask.cards.service;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.testtask.cards.dataaccess.entity.CardEntity;
import ru.testtask.cards.dataaccess.entity.PaymentsEntity;
import ru.testtask.cards.dataaccess.entity.UserEntity;
import ru.testtask.cards.dataaccess.repository.CardRepository;
import ru.testtask.cards.dataaccess.repository.UserRepository;
import ru.testtask.cards.service.entity.Card;
import ru.testtask.cards.service.mapper.CardMapper;
import ru.testtask.cards.utilits.encription.CardEncryptionUtil;
import ru.testtask.cards.utilits.enums.CardStatus;
import ru.testtask.cards.utilits.enums.PaymentStatus;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Data
@AllArgsConstructor
public class CardService {
    private CardRepository repository;
    private UserRepository userRepository;
    private PaymentService paymentService;


    public void create(Card card) {
        //TODO
        repository.save(CardMapper.toData(card));
    }

    public List<Card> getAllUsersCards(Long userId) {
        UserEntity userEntity = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        return userEntity.getCards().stream()
                .map(CardMapper::toService)
                .collect(Collectors.toList());
    }

    public List<CardWithTransactionsDTO> getAllAdminCards() { // получаем все карты со всеми транзакциями
        return repository.findAll()
                .stream()
                .map(card -> {
                    List<PaymentsEntity> allTransactions = Stream.concat(
                                    card.getOutgoingPayments().stream(),
                                    card.getIncomingPayments().stream())
                            .sorted(Comparator.comparing(PaymentsEntity::getTimestamp))
                            .collect(Collectors.toList());
                    return new CardWithTransactionsDTO(CardMapper.toService(card), allTransactions);
                })
                .collect(Collectors.toList());
    }


    public CardWithTransactionsDTO showTransaction(Long userId, String cardNum) {

        UserEntity userEntity = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));


        var encrypt = CardEncryptionUtil.encrypt(cardNum);
        var cardEntity = repository.findCardEntitiesByEncryptedCardNumber(encrypt)
                .orElseThrow(() -> new RuntimeException("Card not found with num: " + cardNum));

        if (!cardEntity.getUser().getId().equals(userId)) {
            throw new RuntimeException("User is not the owner of the card");
        }
        List<PaymentsEntity> allTransactions = Stream.concat(
                        cardEntity.getOutgoingPayments().stream(),
                        cardEntity.getIncomingPayments().stream())
                .sorted(Comparator.comparing(PaymentsEntity::getTimestamp))
                .collect(Collectors.toList());

        return new CardWithTransactionsDTO(CardMapper.toService(cardEntity), allTransactions);

        //TODO
        // сделать преобразование PaymentsEntity в Payments
        // что бы не возвращать сущность базы
    }


    @Transactional
    public void makeTransfer(Long userId, String cardNumberFrom, String cardNumberTo, BigDecimal amount) {

        var userEntity = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        var encryptFrom = CardEncryptionUtil.encrypt(cardNumberFrom);

        var cardFromEntity = repository.findCardEntitiesByEncryptedCardNumber(encryptFrom)
                .orElseThrow(() -> new RuntimeException("Card not found with num: " + cardNumberFrom));

        if (!cardFromEntity.getUser().getId().equals(userId)) {
            throw new RuntimeException("User is not the owner of the card");
        }

        var encryptTo = CardEncryptionUtil.encrypt(cardNumberTo);
        var cardToEntity = repository.findCardEntitiesByEncryptedCardNumber(encryptTo)
                .orElseThrow(() -> new RuntimeException("Card not found with num: " + cardNumberTo));

        if (!cardToEntity.getUser().getId().equals(userId)) {
            throw new RuntimeException("User is not the owner of the card");
        }

        var payment = paymentService.createPendingPayment(cardFromEntity, cardToEntity, amount);

        if (!canMakeTransfer(amount, cardFromEntity, payment, cardToEntity)) return;

        try {
            updateCardAmountAndLimits(amount, cardFromEntity, cardToEntity, payment);

        } catch (RuntimeException e) {
            paymentService.failPayment(payment.getId());
        }
    }

    private boolean canMakeTransfer(BigDecimal amount, CardEntity cardFromEntity, PaymentsEntity payment, CardEntity cardToEntity) {
        // если есть нужная сумма
        if (cardFromEntity.getAmount().compareTo(amount) < 0) {
            paymentService.failPayment(payment.getId());
            return false;
        }
        // если карты активны
        if (cardFromEntity.getStatus() != CardStatus.ACTIVE && cardToEntity.getStatus() != CardStatus.ACTIVE) {
            paymentService.failPayment(payment.getId());
            return false;
        }
        // если сумма не превышает установленные лимиты
        if (amount.compareTo(cardFromEntity.getDailyLimit()) > 0 ||
                amount.compareTo(cardFromEntity.getWeeklyLimit()) > 0 ||
                amount.compareTo(cardFromEntity.getMonthlyLimit()) > 0) {
            paymentService.failPayment(payment.getId());
            return false;
        }
        // если сумма не превышает остаток от лимита
        if (!paymentService.canMakeTransferByLimits(cardFromEntity, amount)) {
            paymentService.failPayment(payment.getId());
            return false;
        }
        return true;
    }

    private void updateCardAmountAndLimits(BigDecimal amount, CardEntity cardFromEntity, CardEntity cardToEntity, PaymentsEntity payment) {
        var newFromAmount = cardFromEntity.getAmount().subtract(amount);
        var newToAmount = cardToEntity.getAmount().add(amount);

        cardFromEntity.setAmount(newFromAmount);
        cardToEntity.setAmount(newToAmount);

        repository.save(cardFromEntity);
        repository.save(cardToEntity);

        payment.setStatus(PaymentStatus.SUCCESS);
        paymentService.savePayment(payment);
    }


}
