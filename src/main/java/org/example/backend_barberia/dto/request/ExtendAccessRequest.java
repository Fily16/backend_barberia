package org.example.backend_barberia.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;
import org.example.backend_barberia.entity.Currency;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ExtendAccessRequest {
    
    @NotNull(message = "El ID de la asignación es requerido")
    private Long userCourseId;
    
    // Duración a extender (en días o meses)
    private Integer durationDays;
    private Integer durationMonths;
    
    // ===== CAMPOS DE PAGO =====
    
    // Monto cobrado al estudiante
    @Positive(message = "El monto debe ser positivo")
    private BigDecimal amount;
    
    // Moneda del pago (PEN o USD)
    private Currency currency;
}
