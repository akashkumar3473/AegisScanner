package com.security.scanner.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.security.scanner.model.Report;
import com.security.scanner.model.Scan;
import com.security.scanner.model.Vulnerability;
import com.security.scanner.repository.ReportRepository;
import com.security.scanner.repository.VulnerabilityRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.awt.Color;
import java.io.*;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ReportService {

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private VulnerabilityRepository vulnerabilityRepository;

    public File generatePdfReport(Scan scan) throws DocumentException, IOException {
        List<Vulnerability> vulnerabilities = vulnerabilityRepository.findByScanId(scan.getId());

        File tempDir = new File("reports");
        if (!tempDir.exists()) {
            tempDir.mkdirs();
        }

        File pdfFile = new File(tempDir, "scan_report_" + scan.getId() + ".pdf");
        Document document = new Document(PageSize.A4, 36, 36, 54, 36);
        PdfWriter.getInstance(document, new FileOutputStream(pdfFile));

        document.open();

        // Title
        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22, Color.DARK_GRAY);
        Paragraph title = new Paragraph("Security & Quality Audit Report", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(20);
        document.add(title);

        // Metadata table
        Font labelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.BLACK);
        Font valueFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.BLACK);

        PdfPTable metaTable = new PdfPTable(2);
        metaTable.setWidthPercentage(100);
        metaTable.setSpacingAfter(15);

        addMetaCell(metaTable, "Repository Name:", scan.getRepository().getName(), labelFont, valueFont);
        addMetaCell(metaTable, "Git URL:", scan.getRepository().getGitUrl(), labelFont, valueFont);
        addMetaCell(metaTable, "Branch:", scan.getRepository().getBranch(), labelFont, valueFont);
        addMetaCell(metaTable, "Scan Date:", scan.getStartedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")), labelFont, valueFont);
        addMetaCell(metaTable, "Scan Status:", scan.getStatus(), labelFont, valueFont);

        document.add(metaTable);

        // Severity summary counters
        Paragraph summaryHeader = new Paragraph("Vulnerability Summary", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, Color.DARK_GRAY));
        summaryHeader.setSpacingAfter(10);
        document.add(summaryHeader);

        PdfPTable statsTable = new PdfPTable(4);
        statsTable.setWidthPercentage(100);
        statsTable.setSpacingAfter(20);

        addStatsCell(statsTable, "CRITICAL", String.valueOf(scan.getCriticalCount()), Color.RED);
        addStatsCell(statsTable, "HIGH", String.valueOf(scan.getHighCount()), Color.ORANGE);
        addStatsCell(statsTable, "MEDIUM", String.valueOf(scan.getMediumCount()), new Color(204, 153, 0));
        addStatsCell(statsTable, "LOW", String.valueOf(scan.getLowCount()), Color.GREEN);

        document.add(statsTable);

        // Code quality metrics summary
        Paragraph qualityHeader = new Paragraph("Code Quality Metrics", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, Color.DARK_GRAY));
        qualityHeader.setSpacingAfter(10);
        document.add(qualityHeader);

        PdfPTable qualityTable = new PdfPTable(4);
        qualityTable.setWidthPercentage(100);
        qualityTable.setSpacingAfter(25);

        addQualityCell(qualityTable, "Maintainability", String.format("%.1f/100", scan.getMaintainabilityIndex()), labelFont);
        addQualityCell(qualityTable, "Complexity", String.valueOf(scan.getCyclomaticComplexity()), labelFont);
        addQualityCell(qualityTable, "Duplicate Code", String.format("%.1f%%", scan.getDuplicateCodePercentage()), labelFont);
        addQualityCell(qualityTable, "Technical Debt", String.format("%d mins", scan.getTechnicalDebtMinutes()), labelFont);

        document.add(qualityTable);

        // Vulnerabilities list table
        Paragraph listHeader = new Paragraph("Vulnerability Findings (" + vulnerabilities.size() + ")", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, Color.DARK_GRAY));
        listHeader.setSpacingAfter(10);
        document.add(listHeader);

        if (vulnerabilities.isEmpty()) {
            document.add(new Paragraph("No vulnerabilities detected in this scan.", valueFont));
        } else {
            PdfPTable vulnTable = new PdfPTable(new float[]{30f, 15f, 45f, 10f});
            vulnTable.setWidthPercentage(100);
            vulnTable.setSpacingAfter(10);

            // Headers
            addVulnHeader(vulnTable, "File", labelFont);
            addVulnHeader(vulnTable, "Severity", labelFont);
            addVulnHeader(vulnTable, "Rule / Finding", labelFont);
            addVulnHeader(vulnTable, "Line", labelFont);

            for (Vulnerability v : vulnerabilities) {
                vulnTable.addCell(new Phrase(v.getFileName(), valueFont));

                PdfPCell sevCell = new PdfPCell(new Phrase(v.getSeverity(), labelFont));
                if ("CRITICAL".equalsIgnoreCase(v.getSeverity())) {
                    sevCell.setBackgroundColor(new Color(255, 230, 230));
                } else if ("HIGH".equalsIgnoreCase(v.getSeverity())) {
                    sevCell.setBackgroundColor(new Color(255, 240, 204));
                } else {
                    sevCell.setBackgroundColor(new Color(240, 240, 240));
                }
                vulnTable.addCell(sevCell);

                vulnTable.addCell(new Phrase(v.getRuleName() + "\n" + v.getDescription(), valueFont));
                vulnTable.addCell(new Phrase(String.valueOf(v.getLineNumber()), valueFont));
            }
            document.add(vulnTable);
        }

        document.close();

        // Save report metadata
        Report report = Report.builder()
                .scan(scan)
                .format("PDF")
                .filePath(pdfFile.getAbsolutePath())
                .build();
        reportRepository.save(report);

        return pdfFile;
    }

    private void addMetaCell(PdfPTable table, String label, String value, Font labelFont, Font valueFont) {
        PdfPCell c1 = new PdfPCell(new Phrase(label, labelFont));
        c1.setBorder(Rectangle.NO_BORDER);
        table.addCell(c1);

        PdfPCell c2 = new PdfPCell(new Phrase(value != null ? value : "N/A", valueFont));
        c2.setBorder(Rectangle.NO_BORDER);
        table.addCell(c2);
    }

    private void addStatsCell(PdfPTable table, String label, String value, Color color) {
        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(new Color(245, 245, 245));
        cell.setPadding(8);

        Paragraph p1 = new Paragraph(label, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, color));
        p1.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(p1);

        Paragraph p2 = new Paragraph(value, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, Color.BLACK));
        p2.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(p2);

        table.addCell(cell);
    }

    private void addQualityCell(PdfPTable table, String label, String value, Font labelFont) {
        PdfPCell cell = new PdfPCell();
        cell.setPadding(8);
        cell.setBackgroundColor(new Color(250, 250, 250));

        Paragraph p1 = new Paragraph(label, FontFactory.getFont(FontFactory.HELVETICA, 8, Color.GRAY));
        p1.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(p1);

        Paragraph p2 = new Paragraph(value, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Color.DARK_GRAY));
        p2.setAlignment(Element.ALIGN_CENTER);
        cell.addElement(p2);

        table.addCell(cell);
    }

    private void addVulnHeader(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(new Color(230, 230, 230));
        cell.setPadding(5);
        table.addCell(cell);
    }

    public String generateCsvReportContent(Scan scan) {
        List<Vulnerability> vulnerabilities = vulnerabilityRepository.findByScanId(scan.getId());
        StringBuilder sb = new StringBuilder();
        sb.append("Vulnerability ID,Rule Name,Severity,File Name,Line Number,Description,Remediation\n");
        for (Vulnerability v : vulnerabilities) {
            sb.append(escapeCsv(String.valueOf(v.getId()))).append(",")
              .append(escapeCsv(v.getRuleName())).append(",")
              .append(escapeCsv(v.getSeverity())).append(",")
              .append(escapeCsv(v.getFileName())).append(",")
              .append(escapeCsv(String.valueOf(v.getLineNumber()))).append(",")
              .append(escapeCsv(v.getDescription())).append(",")
              .append(escapeCsv(v.getFixSuggestion())).append("\n");
        }
        return sb.toString();
    }

    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        String clean = value.replace("\"", "\"\"");
        if (clean.contains(",") || clean.contains("\n") || clean.contains("\"")) {
            return "\"" + clean + "\"";
        }
        return clean;
    }
}
