package ru.testtask.cards.dataaccess.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.testtask.cards.dataaccess.entity.CardEntity;

public interface CardRepository extends JpaRepository<CardEntity, Long> {
}
