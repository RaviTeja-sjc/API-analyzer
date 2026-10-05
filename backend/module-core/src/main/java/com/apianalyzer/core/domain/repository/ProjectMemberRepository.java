package com.apianalyzer.core.domain.repository;
import com.apianalyzer.core.domain.entity.ProjectMember;
import com.apianalyzer.core.domain.entity.ProjectMemberId;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;
public interface ProjectMemberRepository extends JpaRepository<ProjectMember, ProjectMemberId> {
    Optional<ProjectMember> findByProjectIdAndUserId(UUID projectId, UUID userId);
}
