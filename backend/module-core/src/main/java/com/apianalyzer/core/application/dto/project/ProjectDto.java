package com.apianalyzer.core.application.dto.project;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;
public class ProjectDto {
    @Data @Builder @AllArgsConstructor @NoArgsConstructor
    public static class CreateRequest {
        @NotBlank String name;
        String description;
        String repositoryUrl;
    }
    @Data @Builder @AllArgsConstructor @NoArgsConstructor
    public static class UpdateRequest {
        @NotBlank String name;
        String description;
        String repositoryUrl;
    }
    @Data @Builder @AllArgsConstructor @NoArgsConstructor
    public static class MemberRequest {
        @NotNull UUID userId;
        @NotBlank String roleName; // PROJECT_OWNER, DEVELOPER, VIEWER
    }
    @Data @Builder @AllArgsConstructor @NoArgsConstructor
    public static class Response {
        UUID id;
        String name;
        String description;
        String repositoryUrl;
        OffsetDateTime createdAt;
    }
}
