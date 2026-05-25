package com.security.scanner.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "scans")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Scan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "repo_id", nullable = false)
    private Repository repository;

    @Column(nullable = false)
    private String status; // PENDING, RUNNING, COMPLETED, FAILED

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    // Severity Counters
    @Column(name = "critical_count")
    private Integer criticalCount;

    @Column(name = "high_count")
    private Integer highCount;

    @Column(name = "medium_count")
    private Integer mediumCount;

    @Column(name = "low_count")
    private Integer lowCount;

    // Code Quality Metrics
    @Column(name = "cyclomatic_complexity")
    private Integer cyclomaticComplexity;

    @Column(name = "maintainability_index")
    private Double maintainabilityIndex;

    @Column(name = "duplicate_code_percentage")
    private Double duplicateCodePercentage;

    @Column(name = "technical_debt_minutes")
    private Integer technicalDebtMinutes;

    @PrePersist
    protected void onCreate() {
        this.startedAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = "PENDING";
        }
        if (this.criticalCount == null) this.criticalCount = 0;
        if (this.highCount == null) this.highCount = 0;
        if (this.mediumCount == null) this.mediumCount = 0;
        if (this.lowCount == null) this.lowCount = 0;
    }
}
