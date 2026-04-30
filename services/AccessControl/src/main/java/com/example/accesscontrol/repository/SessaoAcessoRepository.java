package com.example.accesscontrol.repository;

import com.example.accesscontrol.entity.SessaoAcesso;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SessaoAcessoRepository extends JpaRepository<SessaoAcesso, UUID> {

    Optional<SessaoAcesso> findByUserIdAndSaidaEmIsNull(UUID userId);
}
