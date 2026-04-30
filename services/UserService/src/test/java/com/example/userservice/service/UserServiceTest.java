package com.example.userservice.service;

import com.example.userservice.dto.AlunoResponse;
import com.example.userservice.dto.FuncionarioResponse;
import com.example.userservice.entity.Aluno;
import com.example.userservice.entity.Funcionario;
import com.example.userservice.repository.AlunoRepository;
import com.example.userservice.repository.FuncionarioRepository;
import com.example.userservice.repository.UserServiceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserServiceRepository userServiceRepository;
    @Mock
    private AlunoRepository alunoRepository;
    @Mock
    private FuncionarioRepository funcionarioRepository;

    @InjectMocks
    private UserService userService;

    @Test
    void listarAlunos_retornaListaMapeadaSemSenha() {
        Aluno aluno = new Aluno();
        aluno.setId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        aluno.setName("João");
        aluno.setEmail("joao@gym.com");
        aluno.setPassword("hashed");
        Date birth = new Date(0);
        Date due = new Date(1000);
        aluno.setBirthDate(birth);
        aluno.setMonthlyPaymentDueDate(due);

        when(alunoRepository.findAll()).thenReturn(List.of(aluno));

        List<AlunoResponse> result = userService.listarAlunos();

        assertEquals(1, result.size());
        AlunoResponse r = result.get(0);
        assertEquals(aluno.getId(), r.getId());
        assertEquals("João", r.getName());
        assertEquals("joao@gym.com", r.getEmail());
        assertEquals(birth, r.getBirthDate());
        assertEquals(due, r.getMonthlyPaymentDueDate());
    }

    @Test
    void listarFuncionarios_retornaListaMapeadaSemSenha() {
        Funcionario func = new Funcionario();
        func.setId(UUID.fromString("00000000-0000-0000-0000-000000000002"));
        func.setName("Maria");
        func.setEmail("maria@gym.com");
        func.setPassword("hashed");
        func.setPosition("Instrutor");
        func.setSalary(3000.0f);

        when(funcionarioRepository.findAll()).thenReturn(List.of(func));

        List<FuncionarioResponse> result = userService.listarFuncionarios();

        assertEquals(1, result.size());
        FuncionarioResponse r = result.get(0);
        assertEquals(func.getId(), r.getId());
        assertEquals("Maria", r.getName());
        assertEquals("maria@gym.com", r.getEmail());
        assertEquals("Instrutor", r.getPosition());
        assertEquals(3000.0f, r.getSalary(), 0.01f);
    }

    @Test
    void listarAlunos_retornaListaVaziaQuandoNaoHaAlunos() {
        when(alunoRepository.findAll()).thenReturn(List.of());

        List<AlunoResponse> result = userService.listarAlunos();

        assertEquals(0, result.size());
    }

    @Test
    void listarFuncionarios_retornaListaVaziaQuandoNaoHaFuncionarios() {
        when(funcionarioRepository.findAll()).thenReturn(List.of());

        List<FuncionarioResponse> result = userService.listarFuncionarios();

        assertEquals(0, result.size());
    }
}
