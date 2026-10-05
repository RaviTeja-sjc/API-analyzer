package com.apianalyzer.core.domain.repository;
import com.apianalyzer.core.domain.entity.Endpoint;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
public interface EndpointRepository extends JpaRepository<Endpoint, UUID> {
}

