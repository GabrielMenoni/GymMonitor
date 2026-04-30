package com.example.accesscontrol.messaging;

import com.example.accesscontrol.dto.AccessEvent;
import com.example.accesscontrol.entity.UserType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccessEventPublisherTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private AccessEventPublisher publisher;

    @Test
    void publish_enviaMensagemParaCheckinRoutingKey() {
        UUID userId = UUID.randomUUID();
        UUID sessaoId = UUID.randomUUID();
        publisher.publish(userId, UserType.ALUNO, "CHECKIN", sessaoId);

        ArgumentCaptor<AccessEvent> captor = ArgumentCaptor.forClass(AccessEvent.class);
        verify(rabbitTemplate).convertAndSend(eq("gymmonitor.access"), eq("access.checkin"), captor.capture());

        AccessEvent event = captor.getValue();
        assertNotNull(event.eventId());
        assertEquals("CHECKIN", event.eventType());
        assertEquals(userId, event.userId());
        assertEquals("ALUNO", event.userType());
        assertNotNull(event.timestamp());
        assertEquals(sessaoId, event.sessaoId());
    }

    @Test
    void publish_enviaMensagemParaCheckoutRoutingKey() {
        UUID userId = UUID.randomUUID();
        UUID sessaoId = UUID.randomUUID();
        publisher.publish(userId, UserType.FUNCIONARIO, "CHECKOUT", sessaoId);

        ArgumentCaptor<AccessEvent> captor = ArgumentCaptor.forClass(AccessEvent.class);
        verify(rabbitTemplate).convertAndSend(eq("gymmonitor.access"), eq("access.checkout"), captor.capture());

        AccessEvent event = captor.getValue();
        assertNotNull(event.eventId());
        assertEquals(userId, event.userId());
        assertNotNull(event.timestamp());
        assertEquals("CHECKOUT", event.eventType());
        assertEquals("FUNCIONARIO", event.userType());
        assertEquals(sessaoId, event.sessaoId());
    }

    @Test
    void publish_naoLancaExcecao_quandoBrokerFalha() {
        UUID userId = UUID.randomUUID();
        UUID sessaoId = UUID.randomUUID();
        doThrow(new RuntimeException("broker indisponivel"))
                .when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(AccessEvent.class));

        assertDoesNotThrow(() -> publisher.publish(userId, UserType.ALUNO, "CHECKIN", sessaoId));
    }
}
