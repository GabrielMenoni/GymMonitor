package com.example.accesscontrol.messaging;

import com.example.accesscontrol.dto.AccessEvent;
import com.example.accesscontrol.entity.UserType;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AccessEventPublisher {

    private static final String EXCHANGE = "gymmonitor.access";
    private static final Logger log = LoggerFactory.getLogger(AccessEventPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    public void publish(UUID userId, UserType userType, String eventType) {
        AccessEvent event = new AccessEvent(
                UUID.randomUUID(),
                eventType,
                userId,
                userType.name(),
                Instant.now().toString()
        );
        String routingKey = "access." + eventType.toLowerCase();
        try {
            rabbitTemplate.convertAndSend(EXCHANGE, routingKey, event);
        } catch (Exception e) {
            log.error("Falha ao publicar evento {} para usuario {}: {}", eventType, userId, e.getMessage());
        }
    }
}
