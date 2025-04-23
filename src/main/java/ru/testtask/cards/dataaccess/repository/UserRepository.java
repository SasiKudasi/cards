package ru.testtask.cards.dataaccess.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.testtask.cards.dataaccess.entity.UserEntity;

import java.util.Optional;

public interface UserRepository extends JpaRepository<UserEntity, Long> {
    Optional<UserEntity> findByEmail(String email);

    Long id(Long id);
}
