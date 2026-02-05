package org.example.backend_barberia.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_course_id")
    private UserCourse userCourse;

    // Monto original cobrado
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    // Moneda original del pago
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Currency currency;

    // Monto convertido a soles (para cálculos del dashboard)
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amountInPen;

    // Tipo de cambio usado en la conversión
    @Column(precision = 10, scale = 4)
    private BigDecimal exchangeRate;

    // Tipo de pago: nuevo o renovación
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentType paymentType;

    // Plan asociado (30 días, 3 meses, etc.)
    @Enumerated(EnumType.STRING)
    private PlanType planType;

    // Duración en días (si aplica)
    private Integer durationDays;

    // Duración en meses (si aplica)
    private Integer durationMonths;

    // Descripción adicional
    private String description;

    // Fecha del pago
    @Column(nullable = false)
    private LocalDateTime paymentDate;

    // Fecha de creación del registro
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (paymentDate == null) {
            paymentDate = LocalDateTime.now();
        }
    }
}
