package com.example.presenceservice.consumer;

import com.example.presenceservice.dto.AccessEvent;
import com.example.presenceservice.service.PresenceService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccessEventConsumerTest {

    @Mock
    private PresenceService presenceService;

    @InjectMocks
    private AccessEventConsumer consumer;

    private AccessEvent buildEvent(String type) {
        return new AccessEvent(UUID.randomUUID(), type, UUID.randomUUID(),
                "ALUNO", Instant.now().toString(), UUID.randomUUID());
    }

    @Test
    void consume_despachaPraHandleCheckin_quandoTipoCheckin() {
        AccessEvent event = buildEvent("CHECKIN");

        consumer.consume(event);

        verify(presenceService).handleCheckin(event);
        verify(presenceService, never()).handleCheckout(any());
    }

    @Test
    void consume_despachaPraHandleCheckout_quandoTipoCheckout() {
        AccessEvent event = buildEvent("CHECKOUT");

        consumer.consume(event);

        verify(presenceService).handleCheckout(event);
        verify(presenceService, never()).handleCheckin(any());
    }

    @Test
    void consume_naoLancaExcecao_quandoTipoDesconhecido() {
        AccessEvent event = buildEvent("UNKNOWN_TYPE");

        assertDoesNotThrow(() -> consumer.consume(event));
        verify(presenceService, never()).handleCheckin(any());
        verify(presenceService, never()).handleCheckout(any());
    }
}
