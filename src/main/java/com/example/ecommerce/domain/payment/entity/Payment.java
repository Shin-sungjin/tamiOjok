package com.example.ecommerce.domain.payment.entity;

import com.example.ecommerce.domain.order.entity.Order;
import com.example.ecommerce.domain.payment.enums.PaymentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "payments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    private Order order;

    @Column(name = "pg_provider", nullable = false)
    private String pgProvider;

    @Column(name = "pg_transaction_id", nullable = false)
    private String pgTransactionId;

    @Column(name = "requested_amount", nullable = false)
    private BigDecimal requestedAmount;

    @Column(name = "paid_amount")
    private BigDecimal paidAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Builder
    private Payment(Order order, String pgProvider, String pgTransactionId, BigDecimal requestedAmount) {
        this.order = order;
        this.pgProvider = pgProvider;
        this.pgTransactionId = pgTransactionId;
        this.requestedAmount = requestedAmount;
        this.status = PaymentStatus.READY;
    }

    public void markPaid(BigDecimal paidAmount) {
        this.paidAmount = paidAmount;
        this.status = PaymentStatus.PAID;
        this.paidAt = LocalDateTime.now();
    }

    public void markFailed() {
        this.status = PaymentStatus.FAILED;
    }

    public void markCancelled() {
        this.status = PaymentStatus.CANCELLED;
    }
}
