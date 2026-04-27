package com.example.accesscontrol.repository;

import com.example.accesscontrol.entity.Aluno;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AlunoRepository extends JpaRepository<Aluno, UUID> {

    Optional<Aluno> findByEmail(String email);

    boolean existsByEmail(String email);
}
