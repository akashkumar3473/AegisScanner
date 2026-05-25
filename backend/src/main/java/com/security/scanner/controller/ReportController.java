package com.security.scanner.controller;

import com.lowagie.text.DocumentException;
import com.security.scanner.model.Report;
import com.security.scanner.model.Scan;
import com.security.scanner.repository.ReportRepository;
import com.security.scanner.repository.ScanRepository;
import com.security.scanner.service.ReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.security.Principal;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    @Autowired
    private ScanRepository scanRepository;

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private ReportService reportService;

    @PostMapping("/generate/{scanId}/pdf")
    public ResponseEntity<?> generatePdfReport(@PathVariable Long scanId, Principal principal) {
        Scan scan = scanRepository.findById(scanId).orElse(null);
        if (scan == null) {
            return ResponseEntity.notFound().build();
        }
        if (!scan.getRepository().getUser().getEmail().equals(principal.getName())) {
            return ResponseEntity.status(403).body("Access denied");
        }

        try {
            File pdf = reportService.generatePdfReport(scan);
            return ResponseEntity.ok(Map.of("message", "PDF generated", "path", pdf.getAbsolutePath()));
        } catch (DocumentException | IOException e) {
            return ResponseEntity.internalServerError().body("Error generating PDF: " + e.getMessage());
        }
    }

    @GetMapping("/download/{scanId}/pdf")
    public ResponseEntity<?> downloadPdf(@PathVariable Long scanId) {
        Scan scan = scanRepository.findById(scanId).orElse(null);
        if (scan == null) {
            return ResponseEntity.notFound().build();
        }

        try {
            Optional<Report> reportOpt = reportRepository.findByScanIdAndFormat(scanId, "PDF");
            File pdfFile;
            if (reportOpt.isPresent()) {
                pdfFile = new File(reportOpt.get().getFilePath());
            } else {
                pdfFile = reportService.generatePdfReport(scan);
            }

            if (!pdfFile.exists()) {
                return ResponseEntity.notFound().build();
            }

            byte[] contents = Files.readAllBytes(pdfFile.toPath());
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", pdfFile.getName());
            return ResponseEntity.ok().headers(headers).body(contents);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error downloading PDF: " + e.getMessage());
        }
    }

    @GetMapping("/download/{scanId}/csv")
    public ResponseEntity<?> downloadCsv(@PathVariable Long scanId) {
        Scan scan = scanRepository.findById(scanId).orElse(null);
        if (scan == null) {
            return ResponseEntity.notFound().build();
        }

        try {
            String csvContent = reportService.generateCsvReportContent(scan);
            byte[] contents = csvContent.getBytes();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("text/csv"));
            headers.setContentDispositionFormData("attachment", "scan_report_" + scanId + ".csv");
            return ResponseEntity.ok().headers(headers).body(contents);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error downloading CSV: " + e.getMessage());
        }
    }
}
