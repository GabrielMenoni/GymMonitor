package com.example.presenceservice.service;

import com.example.presenceservice.dto.AccessEvent;
import com.example.presenceservice.dto.PresenceCountResponse;
import com.example.presenceservice.dto.PresenceUsersResponse;
import com.example.presenceservice.repository.RedisPresenceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class PresenceService {

    private final RedisPresenceRepository repository;

    public void handleCheckin(AccessEvent event) {
        if (repository.isEventProcessed(event.eventId())) {
            log.debug("Evento duplicado ignorado: {}", event.eventId());
            return;
        }
        repository.saveCheckin(event.userId(), event.userType(),
                event.timestamp(), event.sessaoId(), event.eventId());
    }

    public void handleCheckout(AccessEvent event) {
        if (repository.isEventProcessed(event.eventId())) {
            log.debug("Evento duplicado ignorado: {}", event.eventId());
            return;
        }
        repository.removeUser(event.userId());
    }

    public PresenceCountResponse getCount() {
        return new PresenceCountResponse(repository.countInside());
    }

    public PresenceUsersResponse getUsers() {
        return new PresenceUsersResponse(repository.listUsersInside());
    }
}
