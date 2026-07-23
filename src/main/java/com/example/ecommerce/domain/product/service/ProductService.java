package com.example.ecommerce.domain.product.service;

import com.example.ecommerce.domain.product.dto.request.ProductCreateRequest;
import com.example.ecommerce.domain.product.dto.request.ProductUpdateRequest;
import com.example.ecommerce.domain.product.dto.response.ProductResponse;
import com.example.ecommerce.domain.product.entity.Product;
import com.example.ecommerce.domain.product.entity.ProductStock;
import com.example.ecommerce.domain.product.enums.ProductStatus;
import com.example.ecommerce.domain.product.repository.ProductRepository;
import com.example.ecommerce.domain.product.repository.ProductStockRepository;
import com.example.ecommerce.global.exception.CustomException;
import com.example.ecommerce.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductStockRepository productStockRepository;

    @Transactional
    public Long createProduct(ProductCreateRequest request) {
        Product product = Product.builder()
                .name(request.name())
                .price(request.price())
                .description(request.description())
                .status(ProductStatus.ON_SALE)
                .build();
        productRepository.save(product);

        ProductStock stock = ProductStock.builder()
                .product(product)
                .stockQuantity(request.initialStock())
                .build();
        productStockRepository.save(stock);

        return product.getId();
    }

    @Transactional
    public void updateProduct(Long productId, ProductUpdateRequest request) {
        Product product = getProductOrThrow(productId);
        product.update(request.name(), request.price(), request.description());
    }

    @Transactional
    public void changeStatus(Long productId, ProductStatus status) {
        Product product = getProductOrThrow(productId);
        product.changeStatus(status);
    }

    public ProductResponse getProduct(Long productId) {
        Product product = getProductOrThrow(productId);
        ProductStock stock = getStockOrThrow(productId);
        return ProductResponse.of(product, stock);
    }

    public Page<ProductResponse> getOnSaleProducts(Pageable pageable) {
        return productRepository.findByStatus(ProductStatus.ON_SALE, pageable)
                .map(product -> ProductResponse.of(product, getStockOrThrow(product.getId())));
    }

    private Product getProductOrThrow(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));
    }

    private ProductStock getStockOrThrow(Long productId) {
        return productStockRepository.findById(productId)
                .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));
    }
}
