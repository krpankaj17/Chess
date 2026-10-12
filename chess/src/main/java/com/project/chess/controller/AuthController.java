package com.project.chess.controller;

import com.project.chess.dto.AuthResponse;
import com.project.chess.dto.LoginRequest;
import com.project.chess.dto.RegisterRequest;
import com.project.chess.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @GetMapping("/me")
    public ResponseEntity<AuthResponse> me(org.springframework.security.core.Authentication auth) {
        return ResponseEntity.ok(authService.getProfile(auth.getName()));
    }
}
