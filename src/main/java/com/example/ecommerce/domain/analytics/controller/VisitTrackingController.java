package com.example.ecommerce.domain.analytics.controller;

import com.example.ecommerce.domain.analytics.service.SiteVisitService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/track")
@RequiredArgsConstructor
public class VisitTrackingController {

    private final SiteVisitService siteVisitService;

    @PostMapping("/visit")
    public ResponseEntity<Void> trackVisit() {
        siteVisitService.recordVisit();
        return ResponseEntity.noContent().build();
    }
}
