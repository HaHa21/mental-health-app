package com.mentalhealth.mhbackend.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(
    name = "mental_health_record",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_mhr_key",
        columnNames = {"dataset_id", "resource_id", "year", "state", "condition", "metric"}
    )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MentalHealthRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "dataset_id", nullable = false)
    private String datasetId;

    @Column(name = "resource_id", nullable = false)
    private String resourceId;

    @Column(nullable = false)
    private Integer year;

    @Column(nullable = false)
    private String state;

    /** e.g. "Depression", "Anxiety" */
    @Column(name = "condition", nullable = false)
    private String condition;

    /** e.g. "prevalence_rate", "case_count" */
    @Column(nullable = false)
    private String metric;

    @Column(precision = 18, scale = 4)
    private BigDecimal value;

    private String unit;

    @Column(name = "source_url")
    private String sourceUrl;

    @Column(name = "synced_at")
    private Instant syncedAt;
}
