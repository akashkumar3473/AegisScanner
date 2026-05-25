package com.security.scanner.repository;

import com.security.scanner.model.Repository;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface RepositoryRepository extends JpaRepository<Repository, Long> {
    List<Repository> findByUserId(Long userId);
}
