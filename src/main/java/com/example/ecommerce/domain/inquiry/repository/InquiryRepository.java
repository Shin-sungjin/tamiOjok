package com.example.ecommerce.domain.inquiry.repository;

import com.example.ecommerce.domain.inquiry.entity.Inquiry;
import com.example.ecommerce.domain.inquiry.enums.InquiryStatus;
import com.example.ecommerce.domain.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InquiryRepository extends JpaRepository<Inquiry, Long> {

    Page<Inquiry> findByUser(User user, Pageable pageable);

    Page<Inquiry> findByStatus(InquiryStatus status, Pageable pageable);

    long countByStatus(InquiryStatus status);
}
