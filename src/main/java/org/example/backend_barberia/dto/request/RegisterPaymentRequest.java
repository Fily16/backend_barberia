package org.example.backend_barberia.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;
import org.example.backend_barberia.entity.Currency;
import org.example.backend_barberia.entity.PaymentType;
import org.example.backend_barberia.entity.PlanType;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RegisterPaymentRequest {

    @NotNull(message = "El ID del usuario es requerido")
    private Long userId;

    @NotNull(message = "El ID del curso es requerido")
    private Long courseId;

    // ID de la asignación de curso (opcional, se usa para renovaciones)
    private Long userCourseId;

    @NotNull(message = "El monto es requerido")
    @Positive(message = "El monto debe ser positivo")
    private BigDecimal amount;

    @NotNull(message = "La moneda es requerida")
    private Currency currency;  // PEN o USD

    @NotNull(message = "El tipo de pago es requerido")
    private PaymentType paymentType;  // NEW o RENEWAL

    private PlanType planType;  // UNLIMITED o TEMPORAL

    // Duración (para planes temporales)
    private Integer durationDays;
    private Integer durationMonths;

    // Descripción opcional
    private String description;
}
