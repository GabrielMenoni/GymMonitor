package com.example.userservice.controller;

import com.example.userservice.dto.CadastroAdminRequest;
import com.example.userservice.dto.CadastroAlunoRequest;
import com.example.userservice.dto.CadastroFuncionarioRequest;
import com.example.userservice.dto.LoginRequest;
import com.example.userservice.dto.RegisterResponse;
import com.example.userservice.dto.TokenResponse;
import com.example.userservice.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/alunos/cadastro")
    public RegisterResponse cadastrarAluno(@RequestBody CadastroAlunoRequest request) {
        return authService.cadastrarAluno(request);
    }

    @PostMapping("/alunos/login")
    public TokenResponse loginAluno(@RequestBody LoginRequest request) {
        return authService.loginAluno(request);
    }

    @PostMapping("/funcionarios/cadastro")
    public RegisterResponse cadastrarFuncionario(@RequestBody CadastroFuncionarioRequest request) {
        return authService.cadastrarFuncionario(request);
    }

    @PostMapping("/funcionarios/login")
    public TokenResponse loginFuncionario(@RequestBody LoginRequest request) {
        return authService.loginFuncionario(request);
    }

    @PostMapping("/admin/cadastro")
    public RegisterResponse cadastrarAdmin(@RequestBody CadastroAdminRequest request) {
        return authService.cadastrarAdmin(request);
    }

    @PostMapping("/admin/login")
    public TokenResponse loginAdmin(@RequestBody LoginRequest request) {
        return authService.loginAdmin(request);
    }
}
