package com.project.chess;

import com.project.chess.dto.AuthResponse;
import com.project.chess.dto.LoginRequest;
import com.project.chess.dto.RegisterRequest;
import com.project.chess.entity.User;
import com.project.chess.repository.UserRepository;
import com.project.chess.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class AuthServiceTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    void testRegisterAndLogin() {
        RegisterRequest registerReq = RegisterRequest.builder()
                .username("grandmaster")
                .password("securePass123")
                .email("gm@chess.com")
                .build();

        AuthResponse regResponse = authService.register(registerReq);
        assertNotNull(regResponse.getToken());
        assertEquals("grandmaster", regResponse.getUsername());
        assertEquals(1200, regResponse.getEloRating());

        LoginRequest loginReq = LoginRequest.builder()
                .username("grandmaster")
                .password("securePass123")
                .build();

        AuthResponse loginResponse = authService.login(loginReq);
        assertNotNull(loginResponse.getToken());
        assertEquals("grandmaster", loginResponse.getUsername());
        assertEquals(1200, loginResponse.getEloRating());
    }

    @Test
    void testDuplicateRegistrationFails() {
        RegisterRequest registerReq = RegisterRequest.builder()
                .username("player1")
                .password("pass")
                .email("p1@chess.com")
                .build();

        authService.register(registerReq);

        assertThrows(IllegalArgumentException.class, () -> authService.register(registerReq));
    }

    @Test
    void testPasswordIsBcryptHashed() {
        RegisterRequest registerReq = RegisterRequest.builder()
                .username("hashedUser")
                .password("plainTextPassword")
                .email("hash@chess.com")
                .build();

        authService.register(registerReq);

        User savedUser = userRepository.findByUsername("hashedUser").orElseThrow();
        assertNotEquals("plainTextPassword", savedUser.getPassword());
        assertTrue(savedUser.getPassword().startsWith("$2a$") || savedUser.getPassword().startsWith("$2b$"));
    }
}
