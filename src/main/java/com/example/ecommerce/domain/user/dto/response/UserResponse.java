package com.example.ecommerce.domain.user.dto.response;

import com.example.ecommerce.domain.user.entity.User;
import com.example.ecommerce.domain.user.enums.Provider;
import com.example.ecommerce.domain.user.enums.Role;
import com.example.ecommerce.domain.user.enums.UserStatus;

public record UserResponse(
        Long id,
        String email,
        String name,
        String phoneNumber,
        Provider provider,
        Role role,
        UserStatus status
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(), user.getEmail(), user.getName(), user.getPhoneNumber(),
                user.getProvider(), user.getRole(), user.getStatus());
    }
}
