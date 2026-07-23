package com.example.ecommerce.domain.user.repository;

import com.example.ecommerce.domain.user.entity.User;
import com.example.ecommerce.domain.user.entity.UserAddress;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAddressRepository extends JpaRepository<UserAddress, Long> {

    List<UserAddress> findByUser(User user);

    Optional<UserAddress> findByUserAndIsDefaultTrue(User user);

    boolean existsByUser(User user);
}
