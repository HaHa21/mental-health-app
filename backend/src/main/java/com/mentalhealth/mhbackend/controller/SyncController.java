package com.mentalhealth.mhbackend.controller;

import com.mentalhealth.mhbackend.model.SyncLog;
import com.mentalhealth.mhbackend.repository.SyncLogRepository;
import com.mentalhealth.mhbackend.service.MohSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sync")
@RequiredArgsConstructor
public class SyncController {

    private final MohSyncService syncService;
    private final SyncLogRepository syncLogRepository;

    @GetMapping("/trigger")
    @PreAuthorize("hasRole('PROFESSIONAL')")
    public ResponseEntity<SyncLog> trigger() {
        return ResponseEntity.ok(syncService.sync());
    }

    @GetMapping("/status")
    @PreAuthorize("hasRole('PROFESSIONAL')")
    public ResponseEntity<?> status() {
        return syncLogRepository.findTopByOrderBySyncedAtDesc()
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }
}
