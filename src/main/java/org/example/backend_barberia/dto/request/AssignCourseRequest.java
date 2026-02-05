package org.example.backend_barberia.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import org.example.backend_barberia.entity.Currency;
import org.example.backend_barberia.entity.PlanType;

import java.math.BigDecimal;

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
    
    // Días de duración (solo para plan TEMPORAL) - Nueva opción
    private Integer durationDays;
    
    // ===== CAMPOS DE PAGO =====
    
    // Monto cobrado al estudiante
    @Positive(message = "El monto debe ser positivo")
    private BigDecimal amount;
    
    // Moneda del pago (PEN o USD)
    private Currency currency;
}
