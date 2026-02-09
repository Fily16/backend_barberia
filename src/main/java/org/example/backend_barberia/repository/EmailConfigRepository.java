package org.example.backend_barberia.repository;

import org.example.backend_barberia.entity.EmailConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmailConfigRepository extends JpaRepository<EmailConfig, Long> {
    
    default Optional<EmailConfig> getConfig() {
        return findById(1L);
    }
}
