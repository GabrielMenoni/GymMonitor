package com.example.userservice.service;

import com.example.userservice.repository.UserServiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserServiceRepository userServiceRepository;

    public String status() {
        return userServiceRepository.getStatus();
    }
}
