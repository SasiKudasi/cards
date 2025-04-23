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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.WeekFields;
import java.util.Locale;

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


    public boolean canMakeTransferByLimits(CardEntity card, BigDecimal amount){
        var spentToday = repository.sumSuccessfulByCardAndToday(card.getId());

        var now = LocalDate.now();
        var weekFields = WeekFields.of(Locale.getDefault());
        int week = now.get(weekFields.weekOfWeekBasedYear());
        int year = now.getYear();

        var spentThisWeek = repository.sumSuccessfulByCardAndWeek(card.getId(), year, week);
        var spentThisMonth = repository.sumSuccessfulByCardAndMonth(card.getId(), year, now.getMonthValue());

        return spentToday.add(amount).compareTo(card.getDailyLimit()) <= 0 &&
                spentThisWeek.add(amount).compareTo(card.getWeeklyLimit()) <= 0 &&
                spentThisMonth.add(amount).compareTo(card.getMonthlyLimit()) <= 0;
    }
}
