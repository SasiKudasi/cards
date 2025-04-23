package ru.testtask.cards.service.dto;

import ru.testtask.cards.dataaccess.entity.PaymentsEntity;
import ru.testtask.cards.service.entity.Card;

import java.util.List;

//TODO
// СДЕЛАТЬ Payments и мапер из PaymentsEntity в Payments
public record CardWithTransactionsDTO(Card card, List<PaymentsEntity> transactions) {
}
