package com.apianalyzer.core.security;
import com.apianalyzer.core.application.dto.auth.AuthDto.*;
import com.apianalyzer.core.domain.entity.User;
import com.apianalyzer.core.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
@Service
@RequiredArgsConstructor
@SuppressWarnings("null")
public class AuthenticationService {
    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    public AuthResponse register(RegisterRequest request) {
        if(repository.existsByEmail(request.getEmail())) { throw new RuntimeException("Email already exists"); }
        var user = User.builder().fullName(request.getFullName()).email(request.getEmail()).passwordHash(passwordEncoder.encode(request.getPassword())).build();
        repository.save(user);
        return AuthResponse.builder().accessToken(jwtService.generateToken(user)).refreshToken(jwtService.generateRefreshToken(user)).build();
    }
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));
        var user = repository.findByEmail(request.getEmail()).orElseThrow();
        return AuthResponse.builder().accessToken(jwtService.generateToken(user)).refreshToken(jwtService.generateRefreshToken(user)).build();
    }
}
