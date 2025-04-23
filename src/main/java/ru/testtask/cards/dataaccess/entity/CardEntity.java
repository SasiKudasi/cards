package ru.testtask.cards.dataaccess.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.testtask.cards.utilits.enums.CardStatus;


import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "cards")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CardEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    private String encryptedCardNumber;
    String userName;
    LocalDate time;
    @Enumerated(EnumType.STRING)
    CardStatus status;
    BigDecimal amount;
    @OneToMany(mappedBy = "from", cascade = CascadeType.ALL)
    List<PaymentsEntity> outgoingPayments = new ArrayList<>();
    @OneToMany(mappedBy = "to", cascade = CascadeType.ALL)
    List<PaymentsEntity> incomingPayments = new ArrayList<>();
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    UserEntity user;

    //Limits
    private LocalDateTime effectiveDate;
    private BigDecimal dailyLimit;
    private BigDecimal weeklyLimit;
    private BigDecimal monthlyLimit;
}
