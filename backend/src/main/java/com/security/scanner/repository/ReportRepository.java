package com.security.scanner.repository;

import com.security.scanner.model.Report;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ReportRepository extends JpaRepository<Report, Long> {
    List<Report> findByScanId(Long scanId);
    Optional<Report> findByScanIdAndFormat(Long scanId, String format);
}
