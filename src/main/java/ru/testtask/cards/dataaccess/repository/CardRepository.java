package ru.testtask.cards.dataaccess.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.testtask.cards.dataaccess.entity.CardEntity;

import java.util.Optional;

public interface CardRepository extends JpaRepository<CardEntity, Long> {
    Optional<CardEntity> findCardEntitiesByEncryptedCardNumber(String encryptedCardNumber);
}
