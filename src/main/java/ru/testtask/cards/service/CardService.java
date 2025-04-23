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
import ru.testtask.cards.service.dto.CardWithTransactionsDTO;
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
            updateCardAmount(amount, cardFromEntity, cardToEntity, payment);

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
        if (cardFromEntity.getStatus() != CardStatus.ACTIVE) {
            paymentService.failPayment(payment.getId());
            return false;
        }

        if (cardToEntity != null) {
            if (cardToEntity.getStatus() != CardStatus.ACTIVE) {
                paymentService.failPayment(payment.getId());
                return false;
            }
        }

        return true;
    }

    private boolean canWithdraw(BigDecimal amount, CardEntity cardFromEntity, PaymentsEntity payment) {
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

    private void updateCardAmount(BigDecimal amount, CardEntity cardFromEntity, CardEntity cardToEntity, PaymentsEntity payment) {
        var newFromAmount = cardFromEntity.getAmount().subtract(amount);
        var newToAmount = cardToEntity.getAmount().add(amount);

        cardFromEntity.setAmount(newFromAmount);
        cardToEntity.setAmount(newToAmount);

        repository.save(cardFromEntity);
        repository.save(cardToEntity);

        payment.setStatus(PaymentStatus.SUCCESS);
        paymentService.savePayment(payment);
    }


    public void createCardForUser(Long userId, Card card) {
        UserEntity userEntity = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        var cardEntity = CardMapper.toData(card);
        cardEntity.setUser(userEntity);
        repository.save(cardEntity);
    }

    public void blockCard(String cardNumber) {
        var encrypted = CardEncryptionUtil.encrypt(cardNumber);
        var card = repository.findCardEntitiesByEncryptedCardNumber(encrypted)
                .orElseThrow(() -> new RuntimeException("Card not found"));

        card.setStatus(CardStatus.BLOCKED);
        repository.save(card);
    }

    public void activateCard(String cardNumber) {
        var encrypted = CardEncryptionUtil.encrypt(cardNumber);
        var card = repository.findCardEntitiesByEncryptedCardNumber(encrypted)
                .orElseThrow(() -> new RuntimeException("Card not found"));

        card.setStatus(CardStatus.ACTIVE);
        repository.save(card);
    }

    public void deleteCard(String cardNumber) {
        var encrypted = CardEncryptionUtil.encrypt(cardNumber);
        var card = repository.findCardEntitiesByEncryptedCardNumber(encrypted)
                .orElseThrow(() -> new RuntimeException("Card not found"));

        repository.delete(card);
    }

    public void updateLimits(String cardNumber, BigDecimal dailyLimit, BigDecimal weeklyLimit, BigDecimal monthlyLimit) {
        var encrypted = CardEncryptionUtil.encrypt(cardNumber);
        var card = repository.findCardEntitiesByEncryptedCardNumber(encrypted)
                .orElseThrow(() -> new RuntimeException("Card not found"));

        if (dailyLimit != null) card.setDailyLimit(dailyLimit);
        if (weeklyLimit != null) card.setWeeklyLimit(weeklyLimit);
        if (monthlyLimit != null) card.setMonthlyLimit(monthlyLimit);

        repository.save(card);
    }

    @Transactional
    public void withdraw(Long userId, String cardNumber, BigDecimal amount) {
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        var encrypt = CardEncryptionUtil.encrypt(cardNumber);
        var card = repository.findCardEntitiesByEncryptedCardNumber(encrypt)
                .orElseThrow(() -> new RuntimeException("Card not found"));

        if (!card.getUser().getId().equals(userId)) {
            throw new RuntimeException("User is not the owner of the card");
        }

        var payment = paymentService.createPendingPayment(card, null, amount);

        if (!canMakeTransfer(amount, card, payment, null)) return;
        if (!canWithdraw(amount, card, payment)) return;

        try {
            var newDaily = card.getDailyLimit().subtract(amount);
            var newWeekly = card.getWeeklyLimit().subtract(amount);
            var newMonthly = card.getMonthlyLimit().subtract(amount);

            card.setAmount(card.getAmount().subtract(amount));
            card.setDailyLimit(newDaily);
            card.setWeeklyLimit(newWeekly);
            card.setMonthlyLimit(newMonthly);

            repository.save(card);

            payment.setStatus(PaymentStatus.SUCCESS);
            paymentService.savePayment(payment);
        } catch (Exception e) {
            paymentService.failPayment(payment.getId());
        }
    }


}
