package com.example.ecommerce.domain.order.entity;

import com.example.ecommerce.domain.order.enums.OrderStatus;
import com.example.ecommerce.domain.user.entity.User;
import com.example.ecommerce.global.entity.BaseTimeEntity;
import com.example.ecommerce.global.exception.CustomException;
import com.example.ecommerce.global.exception.ErrorCode;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "orders")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id")
    private Long id;

    @Column(name = "order_number", nullable = false, unique = true)
    private String orderNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "total_amount", nullable = false)
    private BigDecimal totalAmount;

    @Column(name = "discount_amount", nullable = false)
    private BigDecimal discountAmount;

    @Column(name = "payment_amount", nullable = false)
    private BigDecimal paymentAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<OrderItem> orderItems = new ArrayList<>();

    @Builder
    private Order(String orderNumber, User user, BigDecimal totalAmount, BigDecimal discountAmount,
                   List<OrderItem> orderItems) {
        this.orderNumber = orderNumber;
        this.user = user;
        this.totalAmount = totalAmount;
        this.discountAmount = discountAmount;
        this.paymentAmount = totalAmount.subtract(discountAmount);
        this.status = OrderStatus.PENDING_PAYMENT;
        this.orderItems = orderItems;
        orderItems.forEach(item -> item.assignOrder(this));
    }

    public boolean isOwnedBy(Long userId) {
        return this.user.getId().equals(userId);
    }

    public void completePayment() {
        requireStatus(OrderStatus.PENDING_PAYMENT, ErrorCode.INVALID_ORDER_STATUS_TRANSITION);
        this.status = OrderStatus.PAYMENT_COMPLETED;
    }

    public void startPreparing() {
        requireStatus(OrderStatus.PAYMENT_COMPLETED, ErrorCode.INVALID_ORDER_STATUS_TRANSITION);
        this.status = OrderStatus.PREPARING;
    }

    public boolean isCancellableWithoutStockReturn() {
        return this.status == OrderStatus.PENDING_PAYMENT;
    }

    public boolean isCancellableWithStockReturn() {
        return this.status == OrderStatus.PAYMENT_COMPLETED || this.status == OrderStatus.PREPARING;
    }

    public void cancel() {
        if (!isCancellableWithoutStockReturn() && !isCancellableWithStockReturn()) {
            throw new CustomException(ErrorCode.ORDER_ALREADY_SHIPPED);
        }
        this.status = OrderStatus.CANCELLED;
    }

    public void expire() {
        requireStatus(OrderStatus.PENDING_PAYMENT, ErrorCode.INVALID_ORDER_STATUS_TRANSITION);
        this.status = OrderStatus.CANCELLED;
    }

    private void requireStatus(OrderStatus expected, ErrorCode errorCode) {
        if (this.status != expected) {
            throw new CustomException(errorCode);
        }
    }
}
