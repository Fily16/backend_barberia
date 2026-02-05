package org.example.backend_barberia.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.backend_barberia.dto.dashboard.DashboardStatsResponse;
import org.example.backend_barberia.dto.dashboard.DashboardStatsResponse.PaymentSummary;
import org.example.backend_barberia.dto.request.RegisterPaymentRequest;
import org.example.backend_barberia.entity.Payment;
import org.example.backend_barberia.service.DashboardService;
import org.example.backend_barberia.service.ExchangeRateService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class DashboardController {

    private final DashboardService dashboardService;
    private final ExchangeRateService exchangeRateService;

    /**
     * Obtiene las estadísticas completas del dashboard
     * GET /api/admin/dashboard/stats
     */
    @GetMapping("/stats")
    public ResponseEntity<DashboardStatsResponse> getDashboardStats() {
        return ResponseEntity.ok(dashboardService.getDashboardStats());
    }

    /**
     * Registra un nuevo pago
     * POST /api/admin/dashboard/payments
     */
    @PostMapping("/payments")
    public ResponseEntity<Map<String, Object>> registerPayment(@Valid @RequestBody RegisterPaymentRequest request) {
        Payment payment = dashboardService.registerPayment(request);
        
        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "Pago registrado exitosamente",
            "paymentId", payment.getId(),
            "amountInPen", payment.getAmountInPen()
        ));
    }

    /**
     * Obtiene el historial de pagos
     * GET /api/admin/dashboard/payments
     * GET /api/admin/dashboard/payments?year=2026&month=2
     */
    @GetMapping("/payments")
    public ResponseEntity<List<PaymentSummary>> getPaymentHistory(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        return ResponseEntity.ok(dashboardService.getPaymentHistory(year, month));
    }

    /**
     * Obtiene el tipo de cambio actual
     * GET /api/admin/dashboard/exchange-rate
     */
    @GetMapping("/exchange-rate")
    public ResponseEntity<ExchangeRateService.ExchangeRateInfo> getExchangeRate() {
        return ResponseEntity.ok(exchangeRateService.getCurrentRateInfo());
    }
}
