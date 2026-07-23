package com.example.ecommerce.domain.cart.repository;

import com.example.ecommerce.domain.cart.entity.Cart;
import com.example.ecommerce.domain.user.entity.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CartRepository extends JpaRepository<Cart, Long> {

    Optional<Cart> findByUser(User user);

    Optional<Cart> findByUser_Id(Long userId);
}
