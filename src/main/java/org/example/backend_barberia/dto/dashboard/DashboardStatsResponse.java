package org.example.backend_barberia.dto.dashboard;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DashboardStatsResponse {

    // ===== RESUMEN DE VENTAS =====
    private BigDecimal totalSalesAllTime;      // Total ventas histórico (en PEN)
    private BigDecimal totalSalesThisMonth;    // Total ventas este mes (en PEN)
    private int totalPaymentsThisMonth;        // Número de pagos este mes
    private int newSubscriptionsThisMonth;     // Nuevas suscripciones este mes
    private int renewalsThisMonth;             // Renovaciones este mes

    // ===== COSTOS DE BUNNY STREAM =====
    private BunnyCostsInfo bunnyCosts;

    // ===== GANANCIAS NETAS =====
    private BigDecimal netProfitThisMonth;     // Ganancia neta (ventas - costos Bunny)
    
    // ===== DISTRIBUCIÓN =====
    private BigDecimal developerShare;         // Tu comisión (16%)
    private BigDecimal barberShare;            // Parte del barbero (84%)
    private BigDecimal developerPercentage;    // Porcentaje del desarrollador
    private BigDecimal barberPercentage;       // Porcentaje del barbero

    // ===== COBERTURA DE COSTOS =====
    private boolean isBunnyCostCovered;        // ¿Alcanza para pagar Bunny?
    private BigDecimal remainingAfterBunny;    // Lo que queda después de pagar Bunny

    // ===== TIPO DE CAMBIO =====
    private BigDecimal exchangeRateUsdToPen;
    private LocalDateTime exchangeRateLastUpdate;

    // ===== HISTORIAL DE PAGOS RECIENTES =====
    private List<PaymentSummary> recentPayments;

    // ===== INFO DEL MES =====
    private int currentMonth;
    private int currentYear;
    private String monthName;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class BunnyCostsInfo {
        private long storageUsedBytes;         // Almacenamiento usado en bytes
        private double storageUsedGb;          // Almacenamiento usado en GB
        private int videoCount;                // Número de videos
        private BigDecimal storageCostUsd;     // Costo de almacenamiento en USD
        private BigDecimal storageCostPen;     // Costo de almacenamiento en PEN
        private BigDecimal estimatedBandwidthCostUsd; // Estimado de bandwidth
        private BigDecimal estimatedBandwidthCostPen; // Estimado de bandwidth en PEN
        private BigDecimal totalCostUsd;       // Costo total en USD
        private BigDecimal totalCostPen;       // Costo total en PEN
        private BigDecimal minimumMonthlyUsd;  // Mínimo mensual de Bunny ($1)
        private BigDecimal minimumMonthlyPen;  // Mínimo mensual en PEN
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class PaymentSummary {
        private Long id;
        private String studentName;
        private String courseName;
        private BigDecimal amount;
        private String currency;
        private BigDecimal amountInPen;
        private String paymentType;
        private String planDescription;
        private LocalDateTime paymentDate;
    }
}
