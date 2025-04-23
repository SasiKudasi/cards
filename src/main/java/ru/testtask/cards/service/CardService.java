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

    public List<Card> getAllCards(Long userId) {
        UserEntity userEntity = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        return userEntity.getCards().stream()
                .map(CardMapper::toService)
                .collect(Collectors.toList());
    }

    public void showTransaction(Long userId, Long cardId) {
        UserEntity userEntity = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        var cardEntity = repository.findById(cardId)
                .orElseThrow(() -> new RuntimeException("Card not found with id: " + cardId));

        if (!cardEntity.getUser().getId().equals(userId)) {
            throw new RuntimeException("User is not the owner of the card");
        }
        List<PaymentsEntity> allTransactions = Stream.concat(
                        cardEntity.getOutgoingPayments().stream(),
                        cardEntity.getIncomingPayments().stream())
                .sorted(Comparator.comparing(PaymentsEntity::getTimestamp))
                .collect(Collectors.toList());

        //TODO
        // сделать преобразование PaymentsEntity в Payments
    }

    @Transactional
    public void makeTransfer(Long userId, Long cardIdFrom, Long cardIdTo, BigDecimal amount) {

        var userEntity = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        var cardFromEntity = repository.findById(cardIdFrom)
                .orElseThrow(() -> new RuntimeException("Card not found with id: " + cardIdFrom));

        var cardToEntity = repository.findById(cardIdTo)
                .orElseThrow(() -> new RuntimeException("Card not found with id: " + cardIdTo));

        if (!cardFromEntity.getUser().getId().equals(userId)) {
            throw new RuntimeException("User is not the owner of the card");
        }

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
        if (cardFromEntity.getAmount().compareTo(amount) < 0) {
            paymentService.failPayment(payment.getId());
            return false;
        }
        if (cardFromEntity.getStatus() != CardStatus.ACTIVE && cardToEntity.getStatus() != CardStatus.ACTIVE) {
            paymentService.failPayment(payment.getId());
            return false;
        }

        if (amount.compareTo(cardFromEntity.getDailyLimit()) > 0 ||
                amount.compareTo(cardFromEntity.getWeeklyLimit()) > 0 ||
                amount.compareTo(cardFromEntity.getMonthlyLimit()) > 0) {
            paymentService.failPayment(payment.getId());
            return false;
        }
        return true;
    }

    private void updateCardAmountAndLimits(BigDecimal amount, CardEntity cardFromEntity, CardEntity cardToEntity, PaymentsEntity payment) {
        var newFromAmount = cardFromEntity.getAmount().subtract(amount);
        var newToAmount = cardToEntity.getAmount().add(amount);

        var dailyLimit = cardFromEntity.getDailyLimit().subtract(amount);
        var weeklyLimit = cardFromEntity.getDailyLimit().subtract(amount);
        var monthlyLimit = cardFromEntity.getDailyLimit().subtract(amount);

        cardFromEntity.setAmount(newFromAmount);
        cardToEntity.setAmount(newToAmount);

        cardFromEntity.setDailyLimit(dailyLimit);
        cardFromEntity.setWeeklyLimit(weeklyLimit);
        cardFromEntity.setMonthlyLimit(monthlyLimit);

        repository.save(cardFromEntity);
        repository.save(cardToEntity);

        payment.setStatus(PaymentStatus.SUCCESS);
        paymentService.savePayment(payment);
    }
}
