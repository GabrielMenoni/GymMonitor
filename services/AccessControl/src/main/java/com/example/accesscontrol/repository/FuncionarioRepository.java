package com.example.accesscontrol.repository;

import com.example.accesscontrol.entity.Funcionario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface FuncionarioRepository extends JpaRepository<Funcionario, UUID> {

    Optional<Funcionario> findByEmail(String email);

    boolean existsByEmail(String email);
}
