package com.security.scanner.service;

import com.security.scanner.model.Scan;
import com.security.scanner.model.Vulnerability;
import com.security.scanner.repository.ScanRepository;
import com.security.scanner.repository.VulnerabilityRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.io.File;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class ScanWorker {

    @Autowired
    private ScanRepository scanRepository;

    @Autowired
    private VulnerabilityRepository vulnerabilityRepository;

    @Autowired
    private GitService gitService;

    @Autowired
    private SastEngine sastEngine;

    @Autowired
    private DependencyScanner dependencyScanner;

    @Autowired
    private CodeQualityEngine codeQualityEngine;

    @Autowired
    private WebSocketService webSocketService;

    @Value("${app.scan.temp-dir}")
    private String tempBaseDir;

    @Transactional
    public void performScan(Long scanId) {
        Scan scan = scanRepository.findById(scanId).orElse(null);
        if (scan == null) {
            return;
        }

        File tempFolder = new File(tempBaseDir + File.separator + UUID.randomUUID());

        try {
            // 1. Update status to RUNNING
            scan.setStatus("RUNNING");
            scanRepository.save(scan);
            webSocketService.sendScanUpdate(scanId, "RUNNING", "Cloning repository...", Map.of());

            // 2. Clone Git Repo
            gitService.cloneRepository(scan.getRepository().getGitUrl(), scan.getRepository().getBranch(), tempFolder);
            webSocketService.sendScanUpdate(scanId, "RUNNING", "Repository cloned. Running SAST Security rules...", Map.of());

            // 3. Perform SAST Scan
            List<Vulnerability> sastFindings = sastEngine.scanDirectory(tempFolder, scan);
            webSocketService.sendScanUpdate(scanId, "RUNNING", "SAST rules applied. Checking dependencies...", Map.of(
                "sastCount", sastFindings.size()
            ));

            // 4. Perform Dependency Scan
            List<Vulnerability> depFindings = dependencyScanner.scanDependencies(tempFolder, scan);
            webSocketService.sendScanUpdate(scanId, "RUNNING", "Dependencies checked. Calculating code metrics...", Map.of(
                "sastCount", sastFindings.size(),
                "dependencyCount", depFindings.size()
            ));

            // 5. Combine and Save all findings
            List<Vulnerability> allFindings = new ArrayList<>();
            allFindings.addAll(sastFindings);
            allFindings.addAll(depFindings);
            vulnerabilityRepository.saveAll(allFindings);

            // 6. Quality Engine Analysis
            CodeQualityEngine.QualityResult qualityResult = codeQualityEngine.analyzeDirectory(tempFolder, allFindings);

            // 7. Update Scan Stats
            int critical = 0, high = 0, medium = 0, low = 0;
            for (Vulnerability v : allFindings) {
                switch (v.getSeverity().toUpperCase()) {
                    case "CRITICAL" -> critical++;
                    case "HIGH" -> high++;
                    case "MEDIUM" -> medium++;
                    case "LOW" -> low++;
                }
            }

            scan.setStatus("COMPLETED");
            scan.setCompletedAt(LocalDateTime.now());
            scan.setCriticalCount(critical);
            scan.setHighCount(high);
            scan.setMediumCount(medium);
            scan.setLowCount(low);
            scan.setCyclomaticComplexity(qualityResult.getTotalComplexity());
            scan.setMaintainabilityIndex(qualityResult.getMaintainabilityIndex());
            scan.setDuplicateCodePercentage(qualityResult.getDuplicatePercentage());
            scan.setTechnicalDebtMinutes(qualityResult.getTechnicalDebtMinutes());

            scanRepository.save(scan);

            webSocketService.sendScanUpdate(scanId, "COMPLETED", "Scan completed successfully", Map.of(
                "critical", critical,
                "high", high,
                "medium", medium,
                "low", low,
                "maintainability", qualityResult.getMaintainabilityIndex(),
                "techDebt", qualityResult.getTechnicalDebtMinutes()
            ));

        } catch (Exception e) {
            scan.setStatus("FAILED");
            scan.setCompletedAt(LocalDateTime.now());
            scanRepository.save(scan);
            webSocketService.sendScanUpdate(scanId, "FAILED", "Scan failed: " + e.getMessage(), Map.of());
            System.err.println("Scan error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            gitService.deleteDirectory(tempFolder);
        }
    }
}
