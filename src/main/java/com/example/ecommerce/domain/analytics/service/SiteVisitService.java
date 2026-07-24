package com.example.ecommerce.domain.analytics.service;

import com.example.ecommerce.domain.analytics.entity.SiteVisit;
import com.example.ecommerce.domain.analytics.repository.SiteVisitRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SiteVisitService {

    private final SiteVisitRepository siteVisitRepository;

    @Transactional
    public void recordVisit() {
        siteVisitRepository.save(new SiteVisit());
    }

    public long countToday() {
        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
        return siteVisitRepository.countByCreatedAtGreaterThanEqual(startOfToday);
    }

    public long countTotal() {
        return siteVisitRepository.count();
    }
}
