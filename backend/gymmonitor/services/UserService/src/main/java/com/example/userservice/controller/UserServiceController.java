package com.example.userservice.controller;

import com.example.userservice.dto.AlunoResponse;
import com.example.userservice.dto.FuncionarioResponse;
import com.example.userservice.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/user-service")
@RequiredArgsConstructor
public class UserServiceController {

    private final UserService userService;

    @GetMapping("/status")
    public String status() {
        return userService.status();
    }

    @GetMapping("/me")
    public Map<String, Object> me(Authentication authentication) {
        return Map.of(
                "email", authentication.getName(),
                "authorities", authentication.getAuthorities());
    }

    @GetMapping("/alunos")
    public List<AlunoResponse> listarAlunos() {
        return userService.listarAlunos();
    }

    @GetMapping("/funcionarios")
    public List<FuncionarioResponse> listarFuncionarios() {
        return userService.listarFuncionarios();
    }
}
