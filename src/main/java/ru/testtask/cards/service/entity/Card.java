package ru.testtask.cards.service.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import ru.testtask.cards.dataaccess.entity.PaymentsEntity;
import ru.testtask.cards.dataaccess.entity.UserEntity;
import ru.testtask.cards.utilits.enums.CardStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


@Getter
@AllArgsConstructor
public class Card {
    private Long id;
    private String cardNumber;
    private String userName;
    private LocalDate time;
    private CardStatus status;
    private BigDecimal amount;
    private List<PaymentsEntity> outgoingPayments = new ArrayList<>();
    private List<PaymentsEntity> incomingPayments = new ArrayList<>();
    private UserEntity user;

    private LocalDateTime effectiveDate = LocalDateTime.now();
    private BigDecimal dailyLimit = new BigDecimal(500);
    private BigDecimal weeklyLimit = new BigDecimal(1500);;
    private BigDecimal monthlyLimit = new BigDecimal(2500);;
}
