package com.security.scanner.controller;

import com.security.scanner.model.Repository;
import com.security.scanner.model.Scan;
import com.security.scanner.model.User;
import com.security.scanner.model.Vulnerability;
import com.security.scanner.repository.RepositoryRepository;
import com.security.scanner.repository.ScanRepository;
import com.security.scanner.repository.UserRepository;
import com.security.scanner.repository.VulnerabilityRepository;
import com.security.scanner.service.ScanQueueService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/scans")
public class ScanController {

    @Autowired
    private ScanRepository scanRepository;

    @Autowired
    private RepositoryRepository repositoryRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private VulnerabilityRepository vulnerabilityRepository;

    @Autowired
    private ScanQueueService scanQueueService;

    @PostMapping("/start")
    public ResponseEntity<?> startScan(@RequestBody Map<String, Long> payload, Principal principal) {
        Long repoId = payload.get("repoId");
        if (repoId == null) {
            return ResponseEntity.badRequest().body("Error: repoId is required");
        }

        Repository repo = repositoryRepository.findById(repoId).orElse(null);
        if (repo == null) {
            return ResponseEntity.badRequest().body("Error: Repository not found");
        }

        if (!repo.getUser().getEmail().equals(principal.getName())) {
            return ResponseEntity.status(403).body("Access denied");
        }

        Scan scan = Scan.builder()
                .repository(repo)
                .status("PENDING")
                .build();

        Scan savedScan = scanRepository.save(scan);
        scanQueueService.queueScan(savedScan.getId());

        return ResponseEntity.ok(savedScan);
    }

    @GetMapping("/user")
    public ResponseEntity<?> getUserScans(Principal principal) {
        User user = userRepository.findByEmail(principal.getName()).orElse(null);
        if (user == null) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(scanRepository.findByRepositoryUserIdOrderByStartedAtDesc(user.getId()));
    }

    @GetMapping("/repo/{repoId}")
    public ResponseEntity<?> getRepoScans(@PathVariable Long repoId, Principal principal) {
        Repository repo = repositoryRepository.findById(repoId).orElse(null);
        if (repo == null) {
            return ResponseEntity.notFound().build();
        }
        if (!repo.getUser().getEmail().equals(principal.getName())) {
            return ResponseEntity.status(403).body("Access denied");
        }
        return ResponseEntity.ok(scanRepository.findByRepositoryIdOrderByStartedAtDesc(repoId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getScan(@PathVariable Long id, Principal principal) {
        Scan scan = scanRepository.findById(id).orElse(null);
        if (scan == null) {
            return ResponseEntity.notFound().build();
        }
        if (!scan.getRepository().getUser().getEmail().equals(principal.getName())) {
            return ResponseEntity.status(403).body("Access denied");
        }
        return ResponseEntity.ok(scan);
    }

    @GetMapping("/{id}/vulnerabilities")
    public ResponseEntity<?> getScanVulnerabilities(@PathVariable Long id, Principal principal) {
        Scan scan = scanRepository.findById(id).orElse(null);
        if (scan == null) {
            return ResponseEntity.notFound().build();
        }
        if (!scan.getRepository().getUser().getEmail().equals(principal.getName())) {
            return ResponseEntity.status(403).body("Access denied");
        }
        return ResponseEntity.ok(vulnerabilityRepository.findByScanId(id));
    }
}
