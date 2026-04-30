package com.example.accesscontrol.service;

import com.example.accesscontrol.dto.CheckinResponse;
import com.example.accesscontrol.dto.CheckoutResponse;
import com.example.accesscontrol.entity.SessaoAcesso;
import com.example.accesscontrol.entity.UserType;
import com.example.accesscontrol.exception.SessaoAbertaException;
import com.example.accesscontrol.exception.SessaoNaoEncontradaException;
import com.example.accesscontrol.messaging.AccessEventPublisher;
import com.example.accesscontrol.repository.SessaoAcessoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccessServiceTest {

    @Mock
    private SessaoAcessoRepository repository;

    @Mock
    private AccessEventPublisher eventPublisher;

    @InjectMocks
    private AccessService accessService;

    private final UUID userId = UUID.randomUUID();

    @Test
    void checkin_criaSessao_quandoNaoHaSessaoAberta() {
        when(repository.findByUserIdAndSaidaEmIsNull(userId)).thenReturn(Optional.empty());
        SessaoAcesso saved = new SessaoAcesso(UUID.randomUUID(), userId, UserType.ALUNO, Instant.now(), null);
        when(repository.save(any(SessaoAcesso.class))).thenReturn(saved);

        CheckinResponse response = accessService.checkin(userId, UserType.ALUNO);

        assertNotNull(response.getSessaoId());
        assertEquals(userId, response.getUserId());
        assertEquals("ALUNO", response.getUserType());
        assertNotNull(response.getEntradaEm());
        verify(repository).save(any(SessaoAcesso.class));
        verify(eventPublisher).publish(userId, UserType.ALUNO, "CHECKIN");
    }

    @Test
    void checkin_lancaExcecao_quandoJaExisteSessaoAberta() {
        SessaoAcesso aberta = new SessaoAcesso(UUID.randomUUID(), userId, UserType.ALUNO, Instant.now(), null);
        when(repository.findByUserIdAndSaidaEmIsNull(userId)).thenReturn(Optional.of(aberta));

        assertThrows(SessaoAbertaException.class, () -> accessService.checkin(userId, UserType.ALUNO));
        verify(repository, never()).save(any());
        verify(eventPublisher, never()).publish(any(), any(), any());
    }

    @Test
    void checkout_fechaSessao_quandoExisteSessaoAberta() {
        Instant entradaEm = Instant.now().minusSeconds(3600);
        SessaoAcesso aberta = new SessaoAcesso(UUID.randomUUID(), userId, UserType.ALUNO, entradaEm, null);
        SessaoAcesso fechada = new SessaoAcesso(aberta.getId(), userId, UserType.ALUNO, entradaEm, Instant.now());
        when(repository.findByUserIdAndSaidaEmIsNull(userId)).thenReturn(Optional.of(aberta));
        when(repository.save(aberta)).thenReturn(fechada);

        CheckoutResponse response = accessService.checkout(userId, UserType.ALUNO);

        assertNotNull(response.getSaidaEm());
        assertEquals(userId, response.getUserId());
        verify(repository).save(aberta);
        verify(eventPublisher).publish(userId, UserType.ALUNO, "CHECKOUT");
    }

    @Test
    void checkout_lancaExcecao_quandoNaoExisteSessaoAberta() {
        when(repository.findByUserIdAndSaidaEmIsNull(userId)).thenReturn(Optional.empty());

        assertThrows(SessaoNaoEncontradaException.class, () -> accessService.checkout(userId, UserType.ALUNO));
        verify(repository, never()).save(any());
        verify(eventPublisher, never()).publish(any(), any(), any());
    }
}
