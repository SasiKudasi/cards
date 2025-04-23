package ru.testtask.cards.dataaccess.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.testtask.cards.utilits.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentsEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    @ManyToOne
    @JoinColumn(name = "from_card_id")
    CardEntity from;
    @ManyToOne
    @JoinColumn(name = "to_card_id")
    CardEntity to;
    LocalDateTime timestamp;
    @Enumerated(EnumType.STRING)
    PaymentStatus status;
    BigDecimal amount;
}
