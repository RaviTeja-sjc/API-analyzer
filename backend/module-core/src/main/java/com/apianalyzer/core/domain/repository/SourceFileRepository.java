package com.apianalyzer.core.domain.repository;
import com.apianalyzer.core.domain.entity.SourceFile;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
public interface SourceFileRepository extends JpaRepository<SourceFile, UUID> {
}

