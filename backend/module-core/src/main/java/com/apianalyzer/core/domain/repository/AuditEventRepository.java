package com.apianalyzer.core.domain.repository;
import com.apianalyzer.core.domain.entity.AuditEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
public interface AuditEventRepository extends JpaRepository<AuditEvent, UUID> {}
