package org.example.backend_barberia.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.backend_barberia.config.BunnyStreamConfig;
import org.example.backend_barberia.dto.dashboard.DashboardStatsResponse;
import org.example.backend_barberia.dto.dashboard.DashboardStatsResponse.BunnyCostsInfo;
import org.example.backend_barberia.dto.dashboard.DashboardStatsResponse.PaymentSummary;
import org.example.backend_barberia.dto.request.RegisterPaymentRequest;
import org.example.backend_barberia.entity.*;
import org.example.backend_barberia.exception.BadRequestException;
import org.example.backend_barberia.exception.ResourceNotFoundException;
import org.example.backend_barberia.repository.CourseRepository;
import org.example.backend_barberia.repository.PaymentRepository;
import org.example.backend_barberia.repository.UserCourseRepository;
import org.example.backend_barberia.repository.UserRepository;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardService {

    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final UserCourseRepository userCourseRepository;
    private final ExchangeRateService exchangeRateService;
    private final BunnyStreamConfig bunnyConfig;
    private final RestTemplate restTemplate;

    // Porcentaje del desarrollador
    private static final BigDecimal DEVELOPER_PERCENTAGE = new BigDecimal("0.16"); // 16%
    private static final BigDecimal BARBER_PERCENTAGE = new BigDecimal("0.84");    // 84%

    // Precios de Bunny Stream (en USD)
    private static final BigDecimal BUNNY_STORAGE_PRICE_PER_GB = new BigDecimal("0.01");  // $0.01/GB
    private static final BigDecimal BUNNY_BANDWIDTH_PRICE_PER_GB = new BigDecimal("0.005"); // $0.005/GB
    private static final BigDecimal BUNNY_MINIMUM_MONTHLY = new BigDecimal("1.00"); // $1 mínimo mensual

    /**
     * Obtiene las estadísticas completas del dashboard
     */
    public DashboardStatsResponse getDashboardStats() {
        LocalDateTime now = LocalDateTime.now();
        int currentMonth = now.getMonthValue();
        int currentYear = now.getYear();

        // Obtener tipo de cambio
        ExchangeRateService.ExchangeRateInfo rateInfo = exchangeRateService.getCurrentRateInfo();
        BigDecimal exchangeRate = rateInfo.getUsdToPen();

        // Calcular ventas
        BigDecimal totalSalesAllTime = paymentRepository.sumTotalAmountInPen();
        BigDecimal totalSalesThisMonth = paymentRepository.sumAmountInPenByMonth(currentYear, currentMonth);
        
        // Contadores del mes
        List<Payment> paymentsThisMonth = paymentRepository.findByMonth(currentYear, currentMonth);
        int totalPaymentsThisMonth = paymentsThisMonth.size();
        long newSubscriptions = paymentRepository.countByPaymentTypeAndMonth(PaymentType.NEW, currentYear, currentMonth);
        long renewals = paymentRepository.countByPaymentTypeAndMonth(PaymentType.RENEWAL, currentYear, currentMonth);

        // Obtener costos de Bunny
        BunnyCostsInfo bunnyCosts = getBunnyCosts(exchangeRate);

        // Calcular ganancia neta (ventas - costos Bunny)
        BigDecimal netProfit = totalSalesThisMonth.subtract(bunnyCosts.getTotalCostPen());
        if (netProfit.compareTo(BigDecimal.ZERO) < 0) {
            netProfit = BigDecimal.ZERO;
        }

        // Calcular distribución
        BigDecimal developerShare = netProfit.multiply(DEVELOPER_PERCENTAGE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal barberShare = netProfit.multiply(BARBER_PERCENTAGE).setScale(2, RoundingMode.HALF_UP);

        // Verificar cobertura de costos
        boolean isCostCovered = totalSalesThisMonth.compareTo(bunnyCosts.getTotalCostPen()) >= 0;
        BigDecimal remainingAfterBunny = totalSalesThisMonth.subtract(bunnyCosts.getTotalCostPen());

        // Obtener pagos recientes
        List<PaymentSummary> recentPayments = paymentRepository.findTop10ByOrderByPaymentDateDesc()
                .stream()
                .map(this::mapToPaymentSummary)
                .collect(Collectors.toList());

        // Nombre del mes en español
        String monthName = Month.of(currentMonth).getDisplayName(TextStyle.FULL, new Locale("es", "PE"));

        return DashboardStatsResponse.builder()
                .totalSalesAllTime(totalSalesAllTime)
                .totalSalesThisMonth(totalSalesThisMonth)
                .totalPaymentsThisMonth(totalPaymentsThisMonth)
                .newSubscriptionsThisMonth((int) newSubscriptions)
                .renewalsThisMonth((int) renewals)
                .bunnyCosts(bunnyCosts)
                .netProfitThisMonth(netProfit)
                .developerShare(developerShare)
                .barberShare(barberShare)
                .developerPercentage(DEVELOPER_PERCENTAGE.multiply(new BigDecimal("100")))
                .barberPercentage(BARBER_PERCENTAGE.multiply(new BigDecimal("100")))
                .isBunnyCostCovered(isCostCovered)
                .remainingAfterBunny(remainingAfterBunny)
                .exchangeRateUsdToPen(exchangeRate)
                .exchangeRateLastUpdate(rateInfo.getLastUpdate())
                .recentPayments(recentPayments)
                .currentMonth(currentMonth)
                .currentYear(currentYear)
                .monthName(monthName)
                .build();
    }

    /**
     * Obtiene los costos de Bunny Stream usando la API
     */
    private BunnyCostsInfo getBunnyCosts(BigDecimal exchangeRate) {
        try {
            // Llamar a la API de Bunny para obtener info de la biblioteca
            // IMPORTANTE: Usar Account API Key, no Stream API Key
            String url = "https://api.bunny.net/videolibrary/" + bunnyConfig.getLibraryId();
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("AccessKey", bunnyConfig.getAccountApiKey()); // Account API Key
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));
            
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            
            log.info("Llamando a Bunny API: {}", url);
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
            
            Map<String, Object> libraryInfo = response.getBody();
            log.info("Respuesta de Bunny API: {}", libraryInfo);
            
            long storageUsedBytes = 0;
            int videoCount = 0;
            long trafficUsage = 0;
            
            if (libraryInfo != null) {
                storageUsedBytes = libraryInfo.get("StorageUsage") != null 
                        ? ((Number) libraryInfo.get("StorageUsage")).longValue() : 0;
                videoCount = libraryInfo.get("VideoCount") != null 
                        ? ((Number) libraryInfo.get("VideoCount")).intValue() : 0;
                trafficUsage = libraryInfo.get("TrafficUsage") != null
                        ? ((Number) libraryInfo.get("TrafficUsage")).longValue() : 0;
                        
                log.info("Bunny Stats - Storage: {} bytes, Videos: {}, Traffic: {} bytes", 
                        storageUsedBytes, videoCount, trafficUsage);
            }
            
            // Convertir bytes a GB
            double storageGb = storageUsedBytes / (1024.0 * 1024.0 * 1024.0);
            double trafficGb = trafficUsage / (1024.0 * 1024.0 * 1024.0);
            
            // Calcular costo de almacenamiento
            BigDecimal storageCostUsd = BUNNY_STORAGE_PRICE_PER_GB
                    .multiply(new BigDecimal(storageGb))
                    .setScale(4, RoundingMode.HALF_UP);
            
            // Calcular costo de bandwidth usando el dato real de TrafficUsage
            // Si no hay dato de traffic, usar estimación
            double bandwidthGb = trafficGb > 0 ? trafficGb : (videoCount * 2.0);
            BigDecimal bandwidthCostUsd = BUNNY_BANDWIDTH_PRICE_PER_GB
                    .multiply(new BigDecimal(bandwidthGb))
                    .setScale(4, RoundingMode.HALF_UP);
            
            // Costo total (mínimo $1)
            BigDecimal totalCostUsd = storageCostUsd.add(bandwidthCostUsd);
            if (totalCostUsd.compareTo(BUNNY_MINIMUM_MONTHLY) < 0) {
                totalCostUsd = BUNNY_MINIMUM_MONTHLY;
            }
            
            // Convertir a PEN
            BigDecimal storageCostPen = exchangeRateService.convertUsdToPen(storageCostUsd);
            BigDecimal bandwidthCostPen = exchangeRateService.convertUsdToPen(bandwidthCostUsd);
            BigDecimal totalCostPen = exchangeRateService.convertUsdToPen(totalCostUsd);
            BigDecimal minimumPen = exchangeRateService.convertUsdToPen(BUNNY_MINIMUM_MONTHLY);
            
            return BunnyCostsInfo.builder()
                    .storageUsedBytes(storageUsedBytes)
                    .storageUsedGb(Math.round(storageGb * 100.0) / 100.0)
                    .videoCount(videoCount)
                    .storageCostUsd(storageCostUsd)
                    .storageCostPen(storageCostPen)
                    .estimatedBandwidthCostUsd(bandwidthCostUsd)
                    .estimatedBandwidthCostPen(bandwidthCostPen)
                    .totalCostUsd(totalCostUsd)
                    .totalCostPen(totalCostPen)
                    .minimumMonthlyUsd(BUNNY_MINIMUM_MONTHLY)
                    .minimumMonthlyPen(minimumPen)
                    .build();
                    
        } catch (Exception e) {
            log.error("Error obteniendo costos de Bunny: {}", e.getMessage());
            
            // Retornar valores por defecto (mínimo de $1)
            BigDecimal minimumPen = exchangeRateService.convertUsdToPen(BUNNY_MINIMUM_MONTHLY);
            
            return BunnyCostsInfo.builder()
                    .storageUsedBytes(0)
                    .storageUsedGb(0)
                    .videoCount(0)
                    .storageCostUsd(BigDecimal.ZERO)
                    .storageCostPen(BigDecimal.ZERO)
                    .estimatedBandwidthCostUsd(BigDecimal.ZERO)
                    .estimatedBandwidthCostPen(BigDecimal.ZERO)
                    .totalCostUsd(BUNNY_MINIMUM_MONTHLY)
                    .totalCostPen(minimumPen)
                    .minimumMonthlyUsd(BUNNY_MINIMUM_MONTHLY)
                    .minimumMonthlyPen(minimumPen)
                    .build();
        }
    }

    /**
     * Registra un nuevo pago
     */
    @Transactional
    public Payment registerPayment(RegisterPaymentRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        
        Course course = courseRepository.findById(request.getCourseId())
                .orElseThrow(() -> new ResourceNotFoundException("Curso no encontrado"));
        
        UserCourse userCourse = null;
        if (request.getUserCourseId() != null) {
            userCourse = userCourseRepository.findById(request.getUserCourseId())
                    .orElse(null);
        }

        // Calcular monto en PEN
        BigDecimal amountInPen;
        BigDecimal exchangeRate = null;
        
        if (request.getCurrency() == Currency.USD) {
            exchangeRate = exchangeRateService.getUsdToPenRate();
            amountInPen = exchangeRateService.convertUsdToPen(request.getAmount());
        } else {
            amountInPen = request.getAmount();
        }

        // Crear descripción si no se proporciona
        String description = request.getDescription();
        if (description == null || description.isEmpty()) {
            description = buildPaymentDescription(request);
        }

        Payment payment = Payment.builder()
                .user(user)
                .course(course)
                .userCourse(userCourse)
                .amount(request.getAmount())
                .currency(request.getCurrency())
                .amountInPen(amountInPen)
                .exchangeRate(exchangeRate)
                .paymentType(request.getPaymentType())
                .planType(request.getPlanType())
                .durationDays(request.getDurationDays())
                .durationMonths(request.getDurationMonths())
                .description(description)
                .paymentDate(LocalDateTime.now())
                .build();

        return paymentRepository.save(payment);
    }

    /**
     * Obtiene el historial de pagos
     */
    public List<PaymentSummary> getPaymentHistory(Integer year, Integer month) {
        List<Payment> payments;
        
        if (year != null && month != null) {
            payments = paymentRepository.findByMonth(year, month);
        } else {
            payments = paymentRepository.findRecentPayments();
        }
        
        return payments.stream()
                .map(this::mapToPaymentSummary)
                .collect(Collectors.toList());
    }

    /**
     * Convierte Payment a PaymentSummary
     */
    private PaymentSummary mapToPaymentSummary(Payment payment) {
        String planDescription = buildPlanDescription(payment);
        
        return PaymentSummary.builder()
                .id(payment.getId())
                .studentName(payment.getUser().getFullName())
                .courseName(payment.getCourse().getTitle())
                .amount(payment.getAmount())
                .currency(payment.getCurrency().name())
                .amountInPen(payment.getAmountInPen())
                .paymentType(payment.getPaymentType() == PaymentType.NEW ? "Nueva suscripción" : "Renovación")
                .planDescription(planDescription)
                .paymentDate(payment.getPaymentDate())
                .build();
    }

    /**
     * Construye la descripción del plan
     */
    private String buildPlanDescription(Payment payment) {
        if (payment.getPlanType() == PlanType.UNLIMITED) {
            return "Plan Ilimitado";
        }
        
        if (payment.getDurationDays() != null && payment.getDurationDays() > 0) {
            return payment.getDurationDays() + " días";
        }
        
        if (payment.getDurationMonths() != null && payment.getDurationMonths() > 0) {
            return payment.getDurationMonths() + (payment.getDurationMonths() == 1 ? " mes" : " meses");
        }
        
        return "Plan Temporal";
    }

    /**
     * Construye la descripción del pago
     */
    private String buildPaymentDescription(RegisterPaymentRequest request) {
        StringBuilder desc = new StringBuilder();
        desc.append(request.getPaymentType() == PaymentType.NEW ? "Nueva suscripción" : "Renovación");
        desc.append(" - ");
        
        if (request.getPlanType() == PlanType.UNLIMITED) {
            desc.append("Plan Ilimitado");
        } else {
            if (request.getDurationDays() != null && request.getDurationDays() > 0) {
                desc.append(request.getDurationDays()).append(" días");
            } else if (request.getDurationMonths() != null && request.getDurationMonths() > 0) {
                desc.append(request.getDurationMonths()).append(request.getDurationMonths() == 1 ? " mes" : " meses");
            }
        }
        
        return desc.toString();
    }
}
