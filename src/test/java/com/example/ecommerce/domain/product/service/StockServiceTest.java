package com.example.ecommerce.domain.product.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.example.ecommerce.domain.product.entity.ProductStock;
import com.example.ecommerce.domain.product.repository.ProductStockRepository;
import com.example.ecommerce.global.exception.CustomException;
import com.example.ecommerce.global.exception.ErrorCode;
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
}
