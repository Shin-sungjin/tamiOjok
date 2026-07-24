package com.example.ecommerce.domain.analytics.repository;

import com.example.ecommerce.domain.analytics.entity.SiteVisit;
import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SiteVisitRepository extends JpaRepository<SiteVisit, Long> {

    long countByCreatedAtGreaterThanEqual(LocalDateTime from);
}
