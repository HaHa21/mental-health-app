package com.mentalhealth.mhbackend.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "sync_log")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SyncLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "synced_at", nullable = false)
    private Instant syncedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SyncStatus status;

    @Column(name = "records_fetched")
    private Integer recordsFetched;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
}
