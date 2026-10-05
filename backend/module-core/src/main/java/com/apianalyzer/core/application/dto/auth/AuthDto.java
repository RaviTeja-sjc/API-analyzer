package com.apianalyzer.core.application.dto.auth;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
public class AuthDto {
    @Data @Builder @AllArgsConstructor @NoArgsConstructor
    public static class RegisterRequest { @NotBlank String fullName; @Email @NotBlank String email; @NotBlank String password; }
    @Data @Builder @AllArgsConstructor @NoArgsConstructor
    public static class LoginRequest { @Email @NotBlank String email; @NotBlank String password; }
    @Data @Builder @AllArgsConstructor @NoArgsConstructor
    public static class AuthResponse { String accessToken; String refreshToken; }
}
