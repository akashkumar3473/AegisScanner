package com.security.scanner.service;

import com.security.scanner.model.Vulnerability;
import lombok.Data;
import org.springframework.stereotype.Service;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;

@Service
public class CodeQualityEngine {

    @Data
    public static class QualityResult {
        private int totalLinesOfCode = 0;
        private int totalComplexity = 0;
        private double averageComplexity = 1.0;
        private double duplicatePercentage = 0.0;
        private double maintainabilityIndex = 100.0;
        private int technicalDebtMinutes = 0;
    }

    public QualityResult analyzeDirectory(File rootDir, List<Vulnerability> vulnerabilities) {
        QualityResult result = new QualityResult();
        if (rootDir == null || !rootDir.exists()) {
            return result;
        }

        List<File> sourceFiles = new ArrayList<>();
        gatherSourceFiles(rootDir, sourceFiles);

        if (sourceFiles.isEmpty()) {
            return result;
        }

        int loc = 0;
        int complexitySum = 0;
        int complexFilesCount = 0;

        // For duplicate calculation
        List<String> allLinesNormalized = new ArrayList<>();
        Map<String, Integer> sequenceHashes = new HashMap<>();
        int duplicateLinesCount = 0;

        for (File file : sourceFiles) {
            try {
                List<String> lines = Files.readAllLines(file.toPath());
                int fileComplexity = calculateCyclomaticComplexity(lines);
                complexitySum += fileComplexity;
                loc += lines.size();

                if (fileComplexity > 10) {
                    complexFilesCount++;
                }

                // Add to normalized list for duplicate detection (sliding window)
                for (String line : lines) {
                    String norm = line.trim();
                    if (!norm.isEmpty()) {
                        allLinesNormalized.add(norm);
                    }
                }
            } catch (IOException e) {
                // Ignore unreadable files
            }
        }

        result.setTotalLinesOfCode(loc);
        result.setTotalComplexity(complexitySum);
        
        double avgComp = sourceFiles.size() > 0 ? (double) complexitySum / sourceFiles.size() : 1.0;
        result.setAverageComplexity(Math.round(avgComp * 100.0) / 100.0);

        // Simple Duplicate Line Block Detection (5-line sliding window)
        int totalNonBlankLines = allLinesNormalized.size();
        if (totalNonBlankLines >= 5) {
            Set<String> duplicatedBlocks = new HashSet<>();
            for (int i = 0; i <= totalNonBlankLines - 5; i++) {
                StringBuilder block = new StringBuilder();
                for (int j = 0; j < 5; j++) {
                    block.append(allLinesNormalized.get(i + j)).append("|");
                }
                String blockKey = block.toString();
                sequenceHashes.put(blockKey, sequenceHashes.getOrDefault(blockKey, 0) + 1);
                if (sequenceHashes.get(blockKey) > 1) {
                    duplicatedBlocks.add(blockKey);
                }
            }

            // Estimate duplicate lines
            for (int i = 0; i <= totalNonBlankLines - 5; i++) {
                StringBuilder block = new StringBuilder();
                for (int j = 0; j < 5; j++) {
                    block.append(allLinesNormalized.get(i + j)).append("|");
                }
                if (duplicatedBlocks.contains(block.toString())) {
                    duplicateLinesCount++;
                }
            }
            double dupPct = loc > 0 ? ((double) duplicateLinesCount / loc) * 100.0 : 0.0;
            result.setDuplicatePercentage(Math.round(dupPct * 100.0) / 100.0);
        }

        // Calculate Technical Debt based on security findings & complexity issues
        int techDebt = 0;
        int critCount = 0, highCount = 0, medCount = 0, lowCount = 0;

        for (Vulnerability v : vulnerabilities) {
            switch (v.getSeverity().toUpperCase()) {
                case "CRITICAL" -> { techDebt += 120; critCount++; }
                case "HIGH" -> { techDebt += 60; highCount++; }
                case "MEDIUM" -> { techDebt += 30; medCount++; }
                case "LOW" -> { techDebt += 15; lowCount++; }
            }
        }

        // Add quality debt: 60 mins for every highly complex file
        techDebt += complexFilesCount * 60;

        // Add quality debt: 120 mins if duplicate percentage is high
        if (result.getDuplicatePercentage() > 10.0) {
            techDebt += 120;
        }

        result.setTechnicalDebtMinutes(techDebt);

        // Maintainability Index = Max(0, Min(100, 100 - complexityFactor - duplicationFactor - vulnerabilityFactor))
        double complexityFactor = avgComp * 2.0;
        double duplicationFactor = result.getDuplicatePercentage() * 0.5;
        double vulnerabilityFactor = (critCount * 8.0) + (highCount * 4.0) + (medCount * 2.0) + (lowCount * 0.5);

        double mi = 100.0 - complexityFactor - duplicationFactor - vulnerabilityFactor;
        result.setMaintainabilityIndex(Math.max(0.0, Math.round(mi * 100.0) / 100.0));

        return result;
    }

    private void gatherSourceFiles(File rootDir, List<File> sourceFiles) {
        File[] files = rootDir.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory()) {
                String name = file.getName();
                if (name.equals(".git") || name.equals("node_modules") || name.equals("target")
                        || name.equals("bin") || name.equals(".idea") || name.equals("build")
                        || name.equals("dist") || name.equals(".gradle")) {
                    continue;
                }
                gatherSourceFiles(file, sourceFiles);
            } else {
                String name = file.getName().toLowerCase();
                if (name.endsWith(".java") || name.endsWith(".py") || name.endsWith(".js")
                        || name.endsWith(".jsx") || name.endsWith(".ts") || name.endsWith(".tsx")) {
                    sourceFiles.add(file);
                }
            }
        }
    }

    private int calculateCyclomaticComplexity(List<String> lines) {
        int complexity = 1; // Base complexity
        for (String line : lines) {
            String clean = line.trim();
            if (clean.startsWith("//") || clean.startsWith("#") || clean.startsWith("/*")) {
                continue; // Ignore comments
            }

            // Count decision keywords
            // Regex-free or simple regex check for keywords
            String[] tokens = clean.split("\\s+");
            for (String token : tokens) {
                if (token.equals("if") || token.equals("for") || token.equals("while")
                        || token.equals("catch") || token.equals("case") || token.equals("&&")
                        || token.equals("||") || token.equals("??")) {
                    complexity++;
                }
            }
            // Check ternary operator
            if (clean.contains("?") && !clean.contains("?") && clean.contains(":")) {
                complexity++;
            }
        }
        return complexity;
    }
}
