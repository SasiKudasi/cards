package ru.testtask.cards.service.mapper;

import ru.testtask.cards.dataaccess.entity.CardEntity;
import ru.testtask.cards.service.entity.Card;
import ru.testtask.cards.utilits.encription.CardEncryptionUtil;


public class CardMapper {

    public static CardEntity toData(Card card) {
        var encrypt = CardEncryptionUtil.encrypt(card.getCardNumber());
        return new CardEntity(
                card.getId(),
                encrypt,
                card.getUserName(),
                card.getTime(),
                card.getStatus(),
                card.getAmount(),
                card.getOutgoingPayments(),
                card.getIncomingPayments(),
                card.getUser()
        );
    }

    public static Card toService(CardEntity entity){
       var decrypt = CardEncryptionUtil.decrypt(entity.getEncryptedCardNumber());
       var mask = CardEncryptionUtil.maskCardNumber(decrypt);
       return new Card(
               entity.getId(),
               mask,
               entity.getUserName(),
               entity.getTime(),
               entity.getStatus(),
               entity.getAmount(),
               entity.getOutgoingPayments(),
               entity.getIncomingPayments(),
               entity.getUser()
       );
    }


}
