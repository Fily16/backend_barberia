package org.example.backend_barberia.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.backend_barberia.entity.PlanType;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserCourseResponse {
    
    private Long id;
    private Long courseId;
    private String courseSlug;
    private String courseTitle;
    private PlanType planType;
    private LocalDateTime expiresAt;
    private Boolean active;
    private Boolean accessValid; // Si el acceso está vigente
    private LocalDateTime assignedAt;
}
