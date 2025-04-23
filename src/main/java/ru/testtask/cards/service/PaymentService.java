package ru.testtask.cards.service;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ru.testtask.cards.dataaccess.entity.CardEntity;
import ru.testtask.cards.dataaccess.entity.PaymentsEntity;
import ru.testtask.cards.dataaccess.repository.PaymentsRepository;
import ru.testtask.cards.utilits.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@AllArgsConstructor
public class PaymentService {
    private PaymentsRepository repository;


    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public PaymentsEntity createPendingPayment(CardEntity from, CardEntity to, BigDecimal amount) {
        PaymentsEntity payment = new PaymentsEntity(
                null,
                from,
                to,
                LocalDateTime.now(),
                PaymentStatus.PENDING,
                amount
        );
        return repository.save(payment);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void failPayment(Long paymentId) {
        PaymentsEntity payment = repository.findById(paymentId).orElse(null);
        if (payment != null) {
            payment.setStatus(PaymentStatus.FAILED);
            repository.save(payment);
        }
    }


    public void savePayment(PaymentsEntity entity){
        repository.save(entity);
    }
}
