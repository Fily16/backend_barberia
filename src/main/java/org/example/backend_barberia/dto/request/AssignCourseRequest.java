package org.example.backend_barberia.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.example.backend_barberia.entity.PlanType;

@Data
public class AssignCourseRequest {
    
    @NotNull(message = "El ID del usuario es requerido")
    private Long userId;
    
    @NotNull(message = "El ID del curso es requerido")
    private Long courseId;
    
    @NotNull(message = "El tipo de plan es requerido")
    private PlanType planType;
    
    // Meses de duración (solo para plan TEMPORAL)
    private Integer durationMonths;
}
