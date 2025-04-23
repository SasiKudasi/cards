package ru.testtask.cards.dataaccess.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.testtask.cards.dataaccess.entity.PaymentsEntity;

import java.math.BigDecimal;

public interface PaymentsRepository extends JpaRepository <PaymentsEntity, Long> {


        @Query("SELECT COALESCE(SUM(p.amount), 0) " +
                "FROM PaymentsEntity p " +
                "WHERE p.from.id = :cardId " +
                "AND p.status = 'SUCCESS' " +
                "AND DATE(p.timestamp) = CURRENT_DATE")
        BigDecimal sumSuccessfulByCardAndToday(@Param("cardId") Long cardId);

        @Query("SELECT COALESCE(SUM(p.amount), 0) " +
                "FROM PaymentsEntity p " +
                "WHERE p.from.id = :cardId " +
                "AND p.status = 'SUCCESS' " +
                "AND FUNCTION('YEAR', p.timestamp) = :year " +
                "AND FUNCTION('WEEK', p.timestamp) = :week")
        BigDecimal sumSuccessfulByCardAndWeek(@Param("cardId") Long cardId,
                                              @Param("year") int year,
                                              @Param("week") int week);

        @Query("SELECT COALESCE(SUM(p.amount), 0) " +
                "FROM PaymentsEntity p " +
                "WHERE p.from.id = :cardId " +
                "AND p.status = 'SUCCESS' " +
                "AND FUNCTION('YEAR', p.timestamp) = :year " +
                "AND FUNCTION('MONTH', p.timestamp) = :month")
        BigDecimal sumSuccessfulByCardAndMonth(@Param("cardId") Long cardId,
                                               @Param("year") int year,
                                               @Param("month") int month);


}
