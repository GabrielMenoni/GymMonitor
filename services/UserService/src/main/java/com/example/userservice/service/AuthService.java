package com.example.userservice.service;

import com.example.userservice.dto.CadastroAdminRequest;
import com.example.userservice.dto.CadastroAlunoRequest;
import com.example.userservice.dto.CadastroFuncionarioRequest;
import com.example.userservice.dto.LoginRequest;
import com.example.userservice.dto.RegisterResponse;
import com.example.userservice.dto.TokenResponse;
import com.example.userservice.entity.Admin;
import com.example.userservice.entity.Aluno;
import com.example.userservice.entity.Funcionario;
import com.example.userservice.exception.AccessControlException;
import com.example.userservice.repository.AdminRepository;
import com.example.userservice.repository.AlunoRepository;
import com.example.userservice.repository.FuncionarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AdminRepository adminRepository;
    private final AlunoRepository alunoRepository;
    private final FuncionarioRepository funcionarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public RegisterResponse cadastrarAdmin(CadastroAdminRequest request) {
        if (adminRepository.existsByEmail(request.getEmail())) {
            throw new AccessControlException("Admin ja cadastrado com este email");
        }

        Admin admin = new Admin();
        admin.setName(request.getName());
        admin.setEmail(request.getEmail());
        admin.setPassword(passwordEncoder.encode(request.getPassword()));
        Admin savedAdmin = adminRepository.save(admin);
        return new RegisterResponse(savedAdmin.getId(), savedAdmin.getName(), savedAdmin.getEmail(), "ADMIN");
    }

    public TokenResponse loginAdmin(LoginRequest request) {
        Admin admin = adminRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AccessControlException("Credenciais invalidas"));

        if (!passwordEncoder.matches(request.getPassword(), admin.getPassword())) {
            throw new AccessControlException("Credenciais invalidas");
        }

        String token = jwtService.generateToken(admin.getEmail(), "ADMIN");
        return new TokenResponse(token, "ADMIN", admin.getEmail());
    }

    public RegisterResponse cadastrarAluno(CadastroAlunoRequest request) {
        if (alunoRepository.existsByEmail(request.getEmail())) {
            throw new AccessControlException("Aluno ja cadastrado com este email");
        }

        Aluno aluno = new Aluno();
        aluno.setName(request.getName());
        aluno.setEmail(request.getEmail());
        aluno.setBirthDate(request.getBirthDate());
        aluno.setMonthlyPaymentDueDate(nextMonthlyDueDate());
        aluno.setPassword(passwordEncoder.encode(request.getPassword()));
        Aluno savedAluno = alunoRepository.save(aluno);
        return new RegisterResponse(savedAluno.getId(), savedAluno.getName(), savedAluno.getEmail(), "ALUNO");
    }

    public TokenResponse loginAluno(LoginRequest request) {
        Aluno aluno = alunoRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AccessControlException("Credenciais invalidas"));

        if (!passwordEncoder.matches(request.getPassword(), aluno.getPassword())) {
            throw new AccessControlException("Credenciais invalidas");
        }

        String token = jwtService.generateToken(aluno.getEmail(), "ALUNO");
        return new TokenResponse(token, "ALUNO", aluno.getEmail());
    }

    public RegisterResponse cadastrarFuncionario(CadastroFuncionarioRequest request) {
        if (funcionarioRepository.existsByEmail(request.getEmail())) {
            throw new AccessControlException("Funcionario ja cadastrado com este email");
        }

        Funcionario funcionario = new Funcionario();
        funcionario.setName(request.getName());
        funcionario.setEmail(request.getEmail());
        funcionario.setPosition(request.getPosition());
        funcionario.setSalary(request.getSalary());
        funcionario.setPassword(passwordEncoder.encode(request.getPassword()));
        Funcionario savedFuncionario = funcionarioRepository.save(funcionario);
        return new RegisterResponse(savedFuncionario.getId(), savedFuncionario.getName(), savedFuncionario.getEmail(),
                "FUNCIONARIO");
    }

    public TokenResponse loginFuncionario(LoginRequest request) {
        Funcionario funcionario = funcionarioRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AccessControlException("Credenciais invalidas"));

        if (!passwordEncoder.matches(request.getPassword(), funcionario.getPassword())) {
            throw new AccessControlException("Credenciais invalidas");
        }

        String token = jwtService.generateToken(funcionario.getEmail(), "FUNCIONARIO");
        return new TokenResponse(token, "FUNCIONARIO", funcionario.getEmail());
    }

    private Date nextMonthlyDueDate() {
        ZonedDateTime dueDate = ZonedDateTime.now(ZoneId.systemDefault()).plusMonths(1);
        return Date.from(dueDate.toInstant());
    }
}
