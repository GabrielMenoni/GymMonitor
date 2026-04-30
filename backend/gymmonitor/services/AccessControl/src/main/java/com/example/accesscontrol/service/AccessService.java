package com.example.accesscontrol.service;

import com.example.accesscontrol.dto.CheckinResponse;
import com.example.accesscontrol.dto.CheckoutResponse;
import com.example.accesscontrol.entity.SessaoAcesso;
import com.example.accesscontrol.entity.UserType;
import com.example.accesscontrol.exception.SessaoAbertaException;
import com.example.accesscontrol.exception.SessaoNaoEncontradaException;
import com.example.accesscontrol.messaging.AccessEventPublisher;
import com.example.accesscontrol.repository.SessaoAcessoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccessService {

    private final SessaoAcessoRepository repository;
    private final AccessEventPublisher eventPublisher;

    @Transactional
    public CheckinResponse checkin(UUID userId, UserType userType) {
        repository.findByUserIdAndSaidaEmIsNull(userId).ifPresent(s -> {
            throw new SessaoAbertaException("Usuario ja possui um check-in em aberto");
        });

        SessaoAcesso sessao = new SessaoAcesso();
        sessao.setUserId(userId);
        sessao.setUserType(userType);
        sessao.setEntradaEm(Instant.now());

        SessaoAcesso saved = repository.save(sessao);
        eventPublisher.publish(saved.getUserId(), saved.getUserType(), "CHECKIN", saved.getId());

        return new CheckinResponse(
                saved.getId(),
                saved.getUserId(),
                saved.getUserType().name(),
                saved.getEntradaEm().toString());
    }

    @Transactional
    public CheckoutResponse checkout(UUID userId, UserType userType) {
        SessaoAcesso sessao = repository.findByUserIdAndSaidaEmIsNull(userId)
                .orElseThrow(() -> new SessaoNaoEncontradaException("Usuario nao possui check-in em aberto"));

        sessao.setSaidaEm(Instant.now());
        SessaoAcesso saved = repository.save(sessao);
        eventPublisher.publish(saved.getUserId(), saved.getUserType(), "CHECKOUT", saved.getId());

        return new CheckoutResponse(
                saved.getId(),
                saved.getUserId(),
                saved.getUserType().name(),
                saved.getEntradaEm().toString(),
                saved.getSaidaEm().toString());
    }
}
