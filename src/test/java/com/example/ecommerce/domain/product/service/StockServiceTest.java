package com.example.ecommerce.domain.product.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.example.ecommerce.domain.product.entity.Product;
import com.example.ecommerce.domain.product.entity.ProductStock;
import com.example.ecommerce.domain.product.enums.ProductStatus;
import com.example.ecommerce.domain.product.repository.ProductStockRepository;
import com.example.ecommerce.global.exception.CustomException;
import com.example.ecommerce.global.exception.ErrorCode;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StockServiceTest {

    @Mock
    private ProductStockRepository productStockRepository;

    @InjectMocks
    private StockService stockService;

    @Test
    void reserve_delegatesToLockedStockRow() {
        ProductStock stock = ProductStock.builder().product(null).stockQuantity(10).build();
        when(productStockRepository.findByProductIdForUpdate(1L)).thenReturn(Optional.of(stock));

        stockService.reserve(1L, 4);

        assertThat(stock.getReservedQuantity()).isEqualTo(4);
    }

    @Test
    void reserve_missingStockRow_throwsProductNotFound() {
        // 재고 조회는 findByProductIdForUpdate(비관적 락)로만 이뤄지므로, 상품은
        // 있는데 재고 행이 없는 데이터 정합성 문제도 이 경로에서 걸러진다.
        when(productStockRepository.findByProductIdForUpdate(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> stockService.reserve(99L, 1))
                .isInstanceOf(CustomException.class)
                .extracting(ex -> ((CustomException) ex).getErrorCode())
                .isEqualTo(ErrorCode.PRODUCT_NOT_FOUND);
    }

    private Product product(ProductStatus status) {
        return Product.builder()
                .name("탐미오족")
                .price(BigDecimal.valueOf(89000))
                .description("")
                .status(status)
                .build();
    }

    @Test
    void restoreStock_revivesOutOfStockProductWhenStockBecomesAvailable() {
        // 관리자 입고든 주문 취소로 인한 복원이든, 재고가 다시 생기면 품절
        // 상태를 그대로 둘 이유가 없음 — 재입고해도 품절이 안 풀리던 버그.
        Product product = product(ProductStatus.OUT_OF_STOCK);
        ProductStock stock = ProductStock.builder().product(product).stockQuantity(0).build();
        when(productStockRepository.findByProductIdForUpdate(1L)).thenReturn(Optional.of(stock));

        stockService.restoreStock(1L, 5);

        assertThat(product.getStatus()).isEqualTo(ProductStatus.ON_SALE);
    }

    @Test
    void restoreStock_doesNotOverrideHiddenStatus() {
        // 판매중지(HIDDEN)는 관리자의 의도적인 선택이라, 재고가 생겨도 자동으로
        // 판매중 상태로 되돌리면 안 됨.
        Product product = product(ProductStatus.HIDDEN);
        ProductStock stock = ProductStock.builder().product(product).stockQuantity(0).build();
        when(productStockRepository.findByProductIdForUpdate(1L)).thenReturn(Optional.of(stock));

        stockService.restoreStock(1L, 5);

        assertThat(product.getStatus()).isEqualTo(ProductStatus.HIDDEN);
    }

    @Test
    void restoreStock_stillZeroAvailable_doesNotRevive() {
        // reservedQuantity가 남아있어서 복원 후에도 availableQuantity가 0이면
        // (예: 다른 예약이 걸려있는 상태) 아직 품절을 풀면 안 됨.
        Product product = product(ProductStatus.OUT_OF_STOCK);
        ProductStock stock = ProductStock.builder().product(product).stockQuantity(0).build();
        stock.reserve(0); // no-op, 그냥 명시적으로 reservedQuantity=0 유지
        when(productStockRepository.findByProductIdForUpdate(1L)).thenReturn(Optional.of(stock));

        stockService.restoreStock(1L, 0);

        assertThat(product.getStatus()).isEqualTo(ProductStatus.OUT_OF_STOCK);
    }
}
