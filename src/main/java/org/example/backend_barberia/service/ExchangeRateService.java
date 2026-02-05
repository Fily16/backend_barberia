package org.example.backend_barberia.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExchangeRateService {

    private final RestTemplate restTemplate;

    // Cache del tipo de cambio para no hacer muchas llamadas
    private BigDecimal cachedRate = new BigDecimal("3.70");
    private LocalDateTime lastFetch = null;
    private static final int CACHE_MINUTES = 60; // Actualizar cada hora

    // Tipo de cambio por defecto si falla la API
    private static final BigDecimal DEFAULT_RATE = new BigDecimal("3.70");

    /**
     * Obtiene el tipo de cambio USD a PEN
     * Usa cache para evitar muchas llamadas a la API
     */
    public BigDecimal getUsdToPenRate() {
        // Si el cache es válido, usarlo
        if (lastFetch != null && lastFetch.plusMinutes(CACHE_MINUTES).isAfter(LocalDateTime.now())) {
            return cachedRate;
        }

        try {
            // Intentar con la API gratuita de exchangerate-api
            String url = "https://api.exchangerate-api.com/v4/latest/USD";
            
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            
            if (response != null && response.containsKey("rates")) {
                @SuppressWarnings("unchecked")
                Map<String, Number> rates = (Map<String, Number>) response.get("rates");
                
                if (rates.containsKey("PEN")) {
                    Number penRate = rates.get("PEN");
                    cachedRate = new BigDecimal(penRate.toString()).setScale(4, RoundingMode.HALF_UP);
                    lastFetch = LocalDateTime.now();
                    log.info("Tipo de cambio actualizado: 1 USD = {} PEN", cachedRate);
                    return cachedRate;
                }
            }
        } catch (Exception e) {
            log.warn("Error obteniendo tipo de cambio, usando cache/default: {}", e.getMessage());
        }

        return cachedRate != null ? cachedRate : DEFAULT_RATE;
    }

    /**
     * Convierte USD a PEN
     */
    public BigDecimal convertUsdToPen(BigDecimal usdAmount) {
        if (usdAmount == null) return BigDecimal.ZERO;
        BigDecimal rate = getUsdToPenRate();
        return usdAmount.multiply(rate).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Retorna el tipo de cambio actual (para mostrar en el frontend)
     */
    public ExchangeRateInfo getCurrentRateInfo() {
        BigDecimal rate = getUsdToPenRate();
        return ExchangeRateInfo.builder()
                .usdToPen(rate)
                .lastUpdate(lastFetch != null ? lastFetch : LocalDateTime.now())
                .source("exchangerate-api.com")
                .build();
    }

    @lombok.Data
    @lombok.Builder
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class ExchangeRateInfo {
        private BigDecimal usdToPen;
        private LocalDateTime lastUpdate;
        private String source;
    }
}
