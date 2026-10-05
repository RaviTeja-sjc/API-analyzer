package com.apianalyzer.core.presentation.controller;
import com.apianalyzer.core.application.dto.auth.AuthDto.*;
import com.apianalyzer.core.domain.entity.User;
import com.apianalyzer.core.security.AuthenticationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthenticationService service;
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) { return ResponseEntity.ok(service.register(request)); }
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) { return ResponseEntity.ok(service.login(request)); }
    @GetMapping("/me")
    public ResponseEntity<User> me(@AuthenticationPrincipal User user) { return ResponseEntity.ok(user); }
}
