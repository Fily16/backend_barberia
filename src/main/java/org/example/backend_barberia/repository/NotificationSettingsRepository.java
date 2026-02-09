package org.example.backend_barberia.repository;

import org.example.backend_barberia.entity.NotificationSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NotificationSettingsRepository extends JpaRepository<NotificationSettings, Long> {
    
    default Optional<NotificationSettings> getSettings() {
        return findById(1L);
    }
    
    default NotificationSettings getOrCreate() {
        return findById(1L).orElseGet(() -> save(NotificationSettings.builder().id(1L).build()));
    }
}
