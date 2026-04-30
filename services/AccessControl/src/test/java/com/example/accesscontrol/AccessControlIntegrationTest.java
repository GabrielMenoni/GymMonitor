package com.example.accesscontrol;

import com.example.accesscontrol.config.RabbitMQTestConfig;
import com.example.accesscontrol.repository.SessaoAcessoRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(RabbitMQTestConfig.class)
class AccessControlIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SessaoAcessoRepository repository;

    private static final String SECRET = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    private String gerarToken(UUID userId, String email, String role) {
        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET));
        return Jwts.builder()
                .subject(email)
                .claim("role", role)
                .claim("userId", userId.toString())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3_600_000))
                .signWith(key)
                .compact();
    }

    private String gerarTokenExpirado(UUID userId, String email, String role) {
        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET));
        return Jwts.builder()
                .subject(email)
                .claim("role", role)
                .claim("userId", userId.toString())
                .issuedAt(new Date(System.currentTimeMillis() - 7_200_000))
                .expiration(new Date(System.currentTimeMillis() - 3_600_000))
                .signWith(key)
                .compact();
    }

    @Test
    void checkin_retorna201_comTokenValido() throws Exception {
        UUID userId = UUID.randomUUID();
        String token = gerarToken(userId, "aluno@test.com", "ALUNO");

        mockMvc.perform(post("/access/checkin")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(userId.toString()))
                .andExpect(jsonPath("$.userType").value("ALUNO"))
                .andExpect(jsonPath("$.entradaEm").exists())
                .andExpect(jsonPath("$.sessaoId").exists());
    }

    @Test
    void checkin_retorna409_quandoJaExisteSessaoAberta() throws Exception {
        UUID userId = UUID.randomUUID();
        String token = gerarToken(userId, "aluno@test.com", "ALUNO");

        mockMvc.perform(post("/access/checkin")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/access/checkin")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void checkout_retorna200_quandoExisteSessaoAberta() throws Exception {
        UUID userId = UUID.randomUUID();
        String token = gerarToken(userId, "aluno@test.com", "ALUNO");

        mockMvc.perform(post("/access/checkin")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/access/checkout")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessaoId").exists())
                .andExpect(jsonPath("$.userId").value(userId.toString()))
                .andExpect(jsonPath("$.saidaEm").exists())
                .andExpect(jsonPath("$.entradaEm").exists());

        assertFalse(repository.findByUserIdAndSaidaEmIsNull(userId).isPresent());
    }

    @Test
    void checkout_retorna404_quandoNaoExisteSessaoAberta() throws Exception {
        UUID userId = UUID.randomUUID();
        String token = gerarToken(userId, "aluno@test.com", "ALUNO");

        mockMvc.perform(post("/access/checkout")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void checkin_retorna401_comTokenExpirado() throws Exception {
        UUID userId = UUID.randomUUID();
        String token = gerarTokenExpirado(userId, "aluno@test.com", "ALUNO");

        mockMvc.perform(post("/access/checkin")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void checkin_retorna401_semToken() throws Exception {
        mockMvc.perform(post("/access/checkin"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void checkin_retorna403_comRoleAdmin() throws Exception {
        UUID userId = UUID.randomUUID();
        String token = gerarToken(userId, "admin@test.com", "ADMIN");

        mockMvc.perform(post("/access/checkin")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void fluxoCompleto_funcionario_checkinECheckout() throws Exception {
        UUID userId = UUID.randomUUID();
        String token = gerarToken(userId, "func@test.com", "FUNCIONARIO");

        mockMvc.perform(post("/access/checkin")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userType").value("FUNCIONARIO"));

        mockMvc.perform(post("/access/checkout")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userType").value("FUNCIONARIO"))
                .andExpect(jsonPath("$.saidaEm").exists());
    }
}
