package com.example.presenceservice.consumer;

import com.example.presenceservice.dto.AccessEvent;
import com.example.presenceservice.service.PresenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AccessEventConsumer {

    private final PresenceService presenceService;

    @RabbitListener(queues = "gymmonitor.presence")
    public void consume(AccessEvent event) {
        switch (event.eventType()) {
            case "CHECKIN" -> presenceService.handleCheckin(event);
            case "CHECKOUT" -> presenceService.handleCheckout(event);
            default -> log.warn("Tipo de evento desconhecido: {}", event.eventType());
        }
    }
}
