package org.example.backend_barberia.repository;

import org.example.backend_barberia.entity.Payment;
import org.example.backend_barberia.entity.PaymentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    // Pagos de un usuario específico
    List<Payment> findByUserIdOrderByPaymentDateDesc(Long userId);

    // Pagos de un curso específico
    List<Payment> findByCourseIdOrderByPaymentDateDesc(Long courseId);

    // Pagos en un rango de fechas
    List<Payment> findByPaymentDateBetweenOrderByPaymentDateDesc(LocalDateTime start, LocalDateTime end);

    // Pagos del mes actual
    @Query("SELECT p FROM Payment p WHERE YEAR(p.paymentDate) = :year AND MONTH(p.paymentDate) = :month ORDER BY p.paymentDate DESC")
    List<Payment> findByMonth(@Param("year") int year, @Param("month") int month);

    // Suma total de pagos en soles
    @Query("SELECT COALESCE(SUM(p.amountInPen), 0) FROM Payment p")
    BigDecimal sumTotalAmountInPen();

    // Suma total de pagos en soles por mes
    @Query("SELECT COALESCE(SUM(p.amountInPen), 0) FROM Payment p WHERE YEAR(p.paymentDate) = :year AND MONTH(p.paymentDate) = :month")
    BigDecimal sumAmountInPenByMonth(@Param("year") int year, @Param("month") int month);

    // Suma total de pagos en soles por rango de fechas
    @Query("SELECT COALESCE(SUM(p.amountInPen), 0) FROM Payment p WHERE p.paymentDate BETWEEN :start AND :end")
    BigDecimal sumAmountInPenByDateRange(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // Contar pagos por tipo
    long countByPaymentType(PaymentType paymentType);

    // Contar pagos por tipo y mes
    @Query("SELECT COUNT(p) FROM Payment p WHERE p.paymentType = :type AND YEAR(p.paymentDate) = :year AND MONTH(p.paymentDate) = :month")
    long countByPaymentTypeAndMonth(@Param("type") PaymentType type, @Param("year") int year, @Param("month") int month);

    // Últimos N pagos
    List<Payment> findTop10ByOrderByPaymentDateDesc();

    // Pagos recientes con límite personalizado
    @Query("SELECT p FROM Payment p ORDER BY p.paymentDate DESC")
    List<Payment> findRecentPayments();

    // Eliminar pagos de un usuario
    void deleteByUserId(Long userId);
}
