package com.example.accesscontrol.service;

import com.example.accesscontrol.repository.AccessControlRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AccessControlService {

    private final AccessControlRepository accessControlRepository;

    public String status() {
        return accessControlRepository.getStatus();
    }
}
