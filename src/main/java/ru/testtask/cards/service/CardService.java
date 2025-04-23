package ru.testtask.cards.service;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

        var payment =  paymentService.createPendingPayment(cardFromEntity, cardToEntity, amount);
        //так же надо обработать лимит
        if (cardFromEntity.getAmount().compareTo(amount) < 0){
            paymentService.failPayment(payment.getId());
            return;
        }
        if (cardFromEntity.getStatus() != CardStatus.ACTIVE && cardToEntity.getStatus() != CardStatus.ACTIVE){
            paymentService.failPayment(payment.getId());
            return;
        }
        try {
            var newFromAmount = cardFromEntity.getAmount().subtract(amount);
            var newToAmount = cardToEntity.getAmount().add(amount);

            cardFromEntity.setAmount(newFromAmount);
            cardToEntity.setAmount(newToAmount);

            repository.save(cardFromEntity);
            repository.save(cardToEntity);
            payment.setStatus(PaymentStatus.SUCCESS);
            paymentService.savePayment(payment);
        } catch (RuntimeException e) {
            paymentService.failPayment(payment.getId());
        }
    }

    // собрать маппер для транзакций и там обработать это



}
