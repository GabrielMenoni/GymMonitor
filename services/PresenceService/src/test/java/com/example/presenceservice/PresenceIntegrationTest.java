package com.example.presenceservice;

import com.example.presenceservice.config.RabbitMQTestConfig;
import com.example.presenceservice.dto.AccessEvent;
import com.example.presenceservice.repository.RedisPresenceRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(RabbitMQTestConfig.class)
class PresenceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private RedisPresenceRepository repository;

    private static final String SECRET = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";
    private static final UUID FIXED_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @BeforeEach
    void setUp() {
        repository.removeUser(FIXED_USER_ID);
    }

    private String gerarTokenAdmin() {
        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET));
        return Jwts.builder()
                .subject("admin@gym.com")
                .claim("role", "ADMIN")
                .claim("userId", UUID.randomUUID().toString())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3_600_000))
                .signWith(key)
                .compact();
    }

    private String gerarTokenAluno() {
        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET));
        return Jwts.builder()
                .subject("aluno@gym.com")
                .claim("role", "ALUNO")
                .claim("userId", UUID.randomUUID().toString())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3_600_000))
                .signWith(key)
                .compact();
    }

    @Test
    void checkin_aumentaContagem_e_checkout_zera() throws Exception {
        UUID sessaoId = UUID.randomUUID();
        String adminToken = gerarTokenAdmin();

        AccessEvent checkin = new AccessEvent(UUID.randomUUID(), "CHECKIN", FIXED_USER_ID,
                "ALUNO", Instant.now().toString(), sessaoId);
        rabbitTemplate.convertAndSend("gymmonitor.access", "access.checkin", checkin);

        Thread.sleep(500);

        mockMvc.perform(get("/presence/count").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(1));

        mockMvc.perform(get("/presence/users").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.users[0].userId").value(FIXED_USER_ID.toString()))
                .andExpect(jsonPath("$.users[0].userType").value("ALUNO"))
                .andExpect(jsonPath("$.users[0].sessaoId").value(sessaoId.toString()));

        AccessEvent checkout = new AccessEvent(UUID.randomUUID(), "CHECKOUT", FIXED_USER_ID,
                "ALUNO", Instant.now().toString(), sessaoId);
        rabbitTemplate.convertAndSend("gymmonitor.access", "access.checkout", checkout);

        Thread.sleep(500);

        mockMvc.perform(get("/presence/count").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(0));
    }

    @Test
    void eventoDuplicado_naoAumentaContagem() throws Exception {
        UUID sessaoId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();

        AccessEvent checkin = new AccessEvent(eventId, "CHECKIN", FIXED_USER_ID,
                "ALUNO", Instant.now().toString(), sessaoId);
        rabbitTemplate.convertAndSend("gymmonitor.access", "access.checkin", checkin);
        rabbitTemplate.convertAndSend("gymmonitor.access", "access.checkin", checkin);

        Thread.sleep(500);

        String adminToken = gerarTokenAdmin();
        mockMvc.perform(get("/presence/count").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(1));
    }

    @Test
    void tokenInvalido_retorna401() throws Exception {
        mockMvc.perform(get("/presence/count").header("Authorization", "Bearer token.invalido.aqui"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void semToken_retorna401() throws Exception {
        mockMvc.perform(get("/presence/count"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenDeAluno_retorna403() throws Exception {
        String alunoToken = gerarTokenAluno();
        mockMvc.perform(get("/presence/count").header("Authorization", "Bearer " + alunoToken))
                .andExpect(status().isForbidden());
    }
}
