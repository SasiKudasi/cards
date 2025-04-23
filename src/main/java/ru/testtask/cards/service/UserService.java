package ru.testtask.cards.service;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.stereotype.Service;
import ru.testtask.cards.dataaccess.repository.UserRepository;

@Service
@Data
@AllArgsConstructor
public class UserService {
    private UserRepository repository;


    // методы авторизации и тд.


}
