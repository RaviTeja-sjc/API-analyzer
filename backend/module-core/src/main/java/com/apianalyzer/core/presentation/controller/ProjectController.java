package com.apianalyzer.core.presentation.controller;
import com.apianalyzer.core.application.dto.project.ProjectDto.*;
import com.apianalyzer.core.application.service.ProjectService;
import com.apianalyzer.core.domain.entity.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;
@RestController
@RequestMapping("/api/v1/projects")
@RequiredArgsConstructor
public class ProjectController {
    private final ProjectService projectService;
    
    @PostMapping
    public ResponseEntity<Response> create(@Valid @RequestBody CreateRequest req, @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(projectService.createProject(req, user));
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<Response> get(@PathVariable UUID id) {
        return ResponseEntity.ok(projectService.getProject(id));
    }
    
    @GetMapping
    public ResponseEntity<Page<Response>> list(@RequestParam(required = false) String search, Pageable pageable) {
        return ResponseEntity.ok(projectService.listProjects(search, pageable));
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<Response> update(@PathVariable UUID id, @Valid @RequestBody UpdateRequest req, @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(projectService.updateProject(id, req, user.getId()));
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id, @AuthenticationPrincipal User user) {
        projectService.deleteProject(id, user.getId());
        return ResponseEntity.noContent().build();
    }
    
    @PostMapping("/{id}/members")
    public ResponseEntity<Void> addMember(@PathVariable UUID id, @Valid @RequestBody MemberRequest req, @AuthenticationPrincipal User user) {
        projectService.addMember(id, req, user.getId());
        return ResponseEntity.ok().build();
    }
}
