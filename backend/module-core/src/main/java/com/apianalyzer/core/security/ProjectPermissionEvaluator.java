package com.apianalyzer.core.security;
import com.apianalyzer.core.domain.entity.ProjectMember;
import com.apianalyzer.core.domain.entity.User;
import com.apianalyzer.core.domain.repository.ProjectMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import java.io.Serializable;
import java.util.Optional;
import java.util.UUID;
@Component
@RequiredArgsConstructor
public class ProjectPermissionEvaluator implements PermissionEvaluator {
    private final ProjectMemberRepository projectMemberRepository;
    
    @Override
    public boolean hasPermission(Authentication authentication, Object targetDomainObject, Object permission) {
        return false; // Not used directly in our pattern
    }
    
    @Override
    public boolean hasPermission(Authentication authentication, Serializable targetId, String targetType, Object permission) {
        if (authentication == null || !authentication.isAuthenticated() || !(authentication.getPrincipal() instanceof User)) {
            return false;
        }
        User user = (User) authentication.getPrincipal();
        String requestedPermission = permission.toString().toUpperCase();
        
        // Global Admin bypass
        boolean isGlobalAdmin = user.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_GLOBAL_ADMIN"));
        if (isGlobalAdmin) return true;
        
        if ("Project".equalsIgnoreCase(targetType) && targetId instanceof UUID) {
            UUID projectId = (UUID) targetId;
            Optional<ProjectMember> memberOpt = projectMemberRepository.findByProjectIdAndUserId(projectId, user.getId());
            
            if (memberOpt.isEmpty()) return false;
            
            String roleName = memberOpt.get().getRole().getName();
            
            return switch (roleName) {
                case "PROJECT_OWNER" -> true;
                case "DEVELOPER" -> requestedPermission.equals("READ") || requestedPermission.equals("WRITE");
                case "VIEWER" -> requestedPermission.equals("READ");
                default -> false;
            };
        }
        return false;
    }
}
