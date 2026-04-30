package com.example.presenceservice.security;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebSocketAuthChannelInterceptorTest {

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private WebSocketAuthChannelInterceptor interceptor;

    @Mock
    private MessageChannel channel;

    private Message<?> buildConnectMessage(String authHeader) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        if (authHeader != null) {
            accessor.addNativeHeader("Authorization", authHeader);
        }
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    @Test
    void preSend_passaAdiante_quandoNaoEhConnect() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setLeaveMutable(true);
        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        Message<?> result = interceptor.preSend(message, channel);

        assertNotNull(result);
        verifyNoInteractions(jwtService);
    }

    @Test
    void preSend_lancaExcecao_quandoHeaderAuthorizationAusente() {
        Message<?> message = buildConnectMessage(null);

        assertThrows(MessagingException.class, () -> interceptor.preSend(message, channel));
    }

    @Test
    void preSend_lancaExcecao_quandoTokenInvalido() {
        when(jwtService.parseClaims("token-invalido")).thenThrow(new RuntimeException("invalid"));
        Message<?> message = buildConnectMessage("Bearer token-invalido");

        assertThrows(MessagingException.class, () -> interceptor.preSend(message, channel));
    }

    @Test
    void preSend_lancaExcecao_quandoRoleNaoEAdmin() {
        Claims claims = mock(Claims.class);
        when(jwtService.parseClaims("token-aluno")).thenReturn(claims);
        when(jwtService.extractRole(claims)).thenReturn("ALUNO");
        Message<?> message = buildConnectMessage("Bearer token-aluno");

        assertThrows(MessagingException.class, () -> interceptor.preSend(message, channel));
    }

    @Test
    void preSend_retornaMensagem_quandoTokenAdminValido() {
        Claims claims = mock(Claims.class);
        when(jwtService.parseClaims("token-admin")).thenReturn(claims);
        when(jwtService.extractRole(claims)).thenReturn("ADMIN");
        Message<?> message = buildConnectMessage("Bearer token-admin");

        Message<?> result = interceptor.preSend(message, channel);

        assertNotNull(result);
    }
}
