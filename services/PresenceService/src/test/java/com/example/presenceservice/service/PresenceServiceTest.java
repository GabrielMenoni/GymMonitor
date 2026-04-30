package com.example.presenceservice.service;

import com.example.presenceservice.dto.AccessEvent;
import com.example.presenceservice.dto.PresenceCountResponse;
import com.example.presenceservice.dto.PresenceUsersResponse;
import com.example.presenceservice.dto.UserPresenceInfo;
import com.example.presenceservice.repository.RedisPresenceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PresenceServiceTest {

    @Mock
    private RedisPresenceRepository repository;

    @InjectMocks
    private PresenceService presenceService;

    private AccessEvent buildEvent(String type) {
        return new AccessEvent(UUID.randomUUID(), type, UUID.randomUUID(),
                "ALUNO", Instant.now().toString(), UUID.randomUUID());
    }

    @Test
    void handleCheckin_salvaDados_quandoEventoNaoEDuplicado() {
        AccessEvent event = buildEvent("CHECKIN");
        when(repository.isEventProcessed(event.eventId())).thenReturn(false);

        presenceService.handleCheckin(event);

        verify(repository).saveCheckin(event.userId(), event.userType(),
                event.timestamp(), event.sessaoId(), event.eventId());
    }

    @Test
    void handleCheckin_ignoraEvento_quandoDuplicado() {
        AccessEvent event = buildEvent("CHECKIN");
        when(repository.isEventProcessed(event.eventId())).thenReturn(true);

        presenceService.handleCheckin(event);

        verify(repository, never()).saveCheckin(any(), any(), any(), any(), any());
    }

    @Test
    void handleCheckout_removeUsuario_quandoEventoNaoEDuplicado() {
        AccessEvent event = buildEvent("CHECKOUT");
        when(repository.isEventProcessed(event.eventId())).thenReturn(false);

        presenceService.handleCheckout(event);

        verify(repository).removeUser(event.userId());
    }

    @Test
    void handleCheckout_ignoraEvento_quandoDuplicado() {
        AccessEvent event = buildEvent("CHECKOUT");
        when(repository.isEventProcessed(event.eventId())).thenReturn(true);

        presenceService.handleCheckout(event);

        verify(repository, never()).removeUser(any());
    }

    @Test
    void getCount_delegaAoRepositorio() {
        when(repository.countInside()).thenReturn(3L);

        PresenceCountResponse response = presenceService.getCount();

        assertEquals(3L, response.count());
    }

    @Test
    void getUsers_delegaAoRepositorio() {
        UserPresenceInfo info = new UserPresenceInfo(UUID.randomUUID(), "ALUNO",
                "2026-01-01T10:00:00Z", UUID.randomUUID());
        when(repository.listUsersInside()).thenReturn(List.of(info));

        PresenceUsersResponse response = presenceService.getUsers();

        assertEquals(1, response.users().size());
        assertEquals(info, response.users().get(0));
    }
}
