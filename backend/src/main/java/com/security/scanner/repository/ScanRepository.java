package com.security.scanner.repository;

import com.security.scanner.model.Scan;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ScanRepository extends JpaRepository<Scan, Long> {
    List<Scan> findByRepositoryIdOrderByStartedAtDesc(Long repoId);
    List<Scan> findByRepositoryUserIdOrderByStartedAtDesc(Long userId);
    List<Scan> findFirst10ByRepositoryUserIdOrderByStartedAtDesc(Long userId);
}
