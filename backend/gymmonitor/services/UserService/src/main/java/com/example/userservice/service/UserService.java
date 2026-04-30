package com.example.userservice.service;

import com.example.userservice.dto.AlunoResponse;
import com.example.userservice.dto.FuncionarioResponse;
import com.example.userservice.repository.AlunoRepository;
import com.example.userservice.repository.FuncionarioRepository;
import com.example.userservice.repository.UserServiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserServiceRepository userServiceRepository;
    private final AlunoRepository alunoRepository;
    private final FuncionarioRepository funcionarioRepository;

    public String status() {
        return userServiceRepository.getStatus();
    }

    @Transactional(readOnly = true)
    public List<AlunoResponse> listarAlunos() {
        return alunoRepository.findAll().stream()
                .map(a -> new AlunoResponse(a.getId(), a.getName(), a.getEmail(),
                        a.getBirthDate(), a.getMonthlyPaymentDueDate()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<FuncionarioResponse> listarFuncionarios() {
        return funcionarioRepository.findAll().stream()
                .map(f -> new FuncionarioResponse(f.getId(), f.getName(), f.getEmail(),
                        f.getPosition(), f.getSalary()))
                .toList();
    }
}
