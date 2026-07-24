package com.example.ecommerce.domain.product.controller;

import com.example.ecommerce.domain.product.dto.request.ProductCreateRequest;
import com.example.ecommerce.domain.product.dto.request.ProductUpdateRequest;
import com.example.ecommerce.domain.product.dto.request.StockAdjustRequest;
import com.example.ecommerce.domain.product.dto.response.ProductResponse;
import com.example.ecommerce.domain.product.enums.ProductStatus;
import com.example.ecommerce.domain.product.service.ProductService;
import com.example.ecommerce.domain.product.service.StockService;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/products")
@RequiredArgsConstructor
public class AdminProductController {

    private final ProductService productService;
    private final StockService stockService;

    @GetMapping
    public ResponseEntity<Page<ProductResponse>> getProducts(
            @RequestParam(required = false) ProductStatus status, Pageable pageable) {
        return ResponseEntity.ok(productService.getProductsForAdmin(status, pageable));
    }

    @PostMapping
    public ResponseEntity<Void> createProduct(@Valid @RequestBody ProductCreateRequest request) {
        Long productId = productService.createProduct(request);
        return ResponseEntity.created(URI.create("/api/v1/products/" + productId)).build();
    }

    @PutMapping("/{productId}")
    public ResponseEntity<Void> updateProduct(@PathVariable Long productId,
                                               @Valid @RequestBody ProductUpdateRequest request) {
        productService.updateProduct(productId, request);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{productId}/status/{status}")
    public ResponseEntity<Void> changeStatus(@PathVariable Long productId, @PathVariable ProductStatus status) {
        productService.changeStatus(productId, status);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{productId}/stock/restock")
    public ResponseEntity<Void> restock(@PathVariable Long productId, @Valid @RequestBody StockAdjustRequest request) {
        stockService.restoreStock(productId, request.quantity());
        return ResponseEntity.noContent().build();
    }
}
