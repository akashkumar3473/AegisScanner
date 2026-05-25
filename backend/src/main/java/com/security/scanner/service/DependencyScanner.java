package com.security.scanner.service;

import com.security.scanner.model.Scan;
import com.security.scanner.model.Vulnerability;
import org.springframework.stereotype.Service;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class DependencyScanner {

    // Vulnerable libraries catalogs
    private static class VulnLib {
        String name; // group:artifact or npm package name
        String maxVulnerableVersion;
        String severity;
        String ruleName;
        String description;
        String remediation;

        VulnLib(String name, String maxVulnerableVersion, String severity, String ruleName, String description, String remediation) {
            this.name = name;
            this.maxVulnerableVersion = maxVulnerableVersion;
            this.severity = severity;
            this.ruleName = ruleName;
            this.description = description;
            this.remediation = remediation;
        }

        boolean isVulnerable(String currentVersion) {
            try {
                // Clean version strings (remove ^, ~, etc.)
                String cleanCur = currentVersion.replaceAll("[^0-9.]", "");
                String cleanMax = maxVulnerableVersion.replaceAll("[^0-9.]", "");
                
                String[] curParts = cleanCur.split("\\.");
                String[] maxParts = cleanMax.split("\\.");
                
                int length = Math.max(curParts.length, maxParts.length);
                for (int i = 0; i < length; i++) {
                    int curPart = i < curParts.length && !curParts[i].isEmpty() ? Integer.parseInt(curParts[i]) : 0;
                    int maxPart = i < maxParts.length && !maxParts[i].isEmpty() ? Integer.parseInt(maxParts[i]) : 0;
                    
                    if (curPart < maxPart) return true;
                    if (curPart > maxPart) return false;
                }
                return true; // if equal, it is vulnerable
            } catch (NumberFormatException e) {
                return false; // Fallback
            }
        }
    }

    private final List<VulnLib> mavenDb = new ArrayList<>();
    private final List<VulnLib> npmDb = new ArrayList<>();

    public DependencyScanner() {
        // Maven Vulnerability DB
        mavenDb.add(new VulnLib(
                "org.apache.logging.log4j:log4j-core", "2.14.1", "CRITICAL",
                "DEPENDENCY_LOG4SHELL",
                "Log4j-core is vulnerable to Log4Shell (CVE-2021-44228) Remote Code Execution.",
                "Upgrade org.apache.logging.log4j:log4j-core to 2.15.0 or higher."
        ));
        mavenDb.add(new VulnLib(
                "org.springframework:spring-beans", "5.3.17", "CRITICAL",
                "DEPENDENCY_SPRING4SHELL",
                "Spring Beans is vulnerable to Spring4Shell (CVE-2022-22965) Remote Code Execution.",
                "Upgrade org.springframework:spring-beans to 5.3.18 or higher."
        ));
        mavenDb.add(new VulnLib(
                "com.fasterxml.jackson.core:jackson-databind", "2.13.2", "HIGH",
                "DEPENDENCY_JACKSON_RCE",
                "Jackson databind version contains deserialization flaws allowing RCE.",
                "Upgrade com.fasterxml.jackson.core:jackson-databind to 2.13.2.1 or 2.14+."
        ));
        mavenDb.add(new VulnLib(
                "com.alibaba:fastjson", "1.2.80", "CRITICAL",
                "DEPENDENCY_FASTJSON_RCE",
                "Alibaba Fastjson contains critical remote code execution capabilities.",
                "Upgrade com.alibaba:fastjson to 1.2.83 or migrate to Gson/Jackson."
        ));

        // NPM Vulnerability DB
        npmDb.add(new VulnLib(
                "lodash", "4.17.20", "HIGH",
                "DEPENDENCY_LODASH_PROTOTYPE_POLLUTION",
                "Lodash is vulnerable to Prototype Pollution (CVE-2020-8203) leading to RCE.",
                "Upgrade lodash to 4.17.21 or higher."
        ));
        npmDb.add(new VulnLib(
                "express", "4.15.5", "MEDIUM",
                "DEPENDENCY_EXPRESS_REDIRECT",
                "Express contains an open redirect vulnerability (CVE-2018-3774).",
                "Upgrade express to 4.16.0 or higher."
        ));
        npmDb.add(new VulnLib(
                "minimist", "1.2.5", "HIGH",
                "DEPENDENCY_MINIMIST_PROTOTYPE_POLLUTION",
                "Minimist package is vulnerable to prototype pollution.",
                "Upgrade minimist to 1.2.6 or higher."
        ));
        npmDb.add(new VulnLib(
                "axios", "0.21.0", "HIGH",
                "DEPENDENCY_AXIOS_SSRF",
                "Axios contains Server-Side Request Forgery vulnerability (CVE-2020-28168).",
                "Upgrade axios to 0.21.1 or higher."
        ));
        npmDb.add(new VulnLib(
                "moment", "2.29.3", "HIGH",
                "DEPENDENCY_MOMENT_DOS",
                "Moment.js is vulnerable to Path Traversal and ReDoS (CVE-2022-31129).",
                "Upgrade moment to 2.29.4 or higher."
        ));
    }

    public List<Vulnerability> scanDependencies(File rootDir, Scan scan) {
        List<Vulnerability> findings = new ArrayList<>();
        if (rootDir == null || !rootDir.exists()) {
            return findings;
        }
        findAndScanManifests(rootDir, rootDir, findings, scan);
        return findings;
    }

    private void findAndScanManifests(File rootDir, File currentFile, List<Vulnerability> findings, Scan scan) {
        String name = currentFile.getName();
        if (currentFile.isDirectory()) {
            if (name.equals(".git") || name.equals("node_modules") || name.equals("target")
                    || name.equals("bin") || name.equals(".idea") || name.equals("build")) {
                return;
            }
            File[] children = currentFile.listFiles();
            if (children != null) {
                for (File child : children) {
                    findAndScanManifests(rootDir, child, findings, scan);
                }
            }
        } else {
            if (name.equals("pom.xml")) {
                scanPomXml(rootDir, currentFile, findings, scan);
            } else if (name.equals("package.json")) {
                scanPackageJson(rootDir, currentFile, findings, scan);
            }
        }
    }

    private void scanPomXml(File rootDir, File pomFile, List<Vulnerability> findings, Scan scan) {
        try {
            String content = Files.readString(pomFile.toPath());
            String relativePath = rootDir.toPath().relativize(pomFile.toPath()).toString().replace('\\', '/');

            // Find all <dependency> blocks
            Pattern depPattern = Pattern.compile("<dependency>[\\s\\S]*?<groupId>([^<]+)</groupId>[\\s\\S]*?<artifactId>([^<]+)</artifactId>[\\s\\S]*?<version>([^<]+)</version>[\\s\\S]*?</dependency>");
            Matcher matcher = depPattern.matcher(content);

            while (matcher.find()) {
                String group = matcher.group(1).trim();
                String artifact = matcher.group(2).trim();
                String version = matcher.group(3).trim();
                String libraryName = group + ":" + artifact;

                for (VulnLib vuln : mavenDb) {
                    if (vuln.name.equalsIgnoreCase(libraryName) && vuln.isVulnerable(version)) {
                        Vulnerability v = Vulnerability.builder()
                                .scan(scan)
                                .fileName(relativePath)
                                .lineNumber(getLineOfSubstring(content, matcher.group(0)))
                                .severity(vuln.severity)
                                .ruleName(vuln.ruleName)
                                .description("Vulnerable package " + libraryName + "@" + version + " found. " + vuln.description)
                                .codeSnippet(matcher.group(0))
                                .fixSuggestion(vuln.remediation)
                                .build();
                        findings.add(v);
                    }
                }
            }
        } catch (IOException e) {
            // Ignore
        }
    }

    private void scanPackageJson(File rootDir, File packageJsonFile, List<Vulnerability> findings, Scan scan) {
        try {
            String content = Files.readString(packageJsonFile.toPath());
            String relativePath = rootDir.toPath().relativize(packageJsonFile.toPath()).toString().replace('\\', '/');

            // Find dependencies section
            Pattern depsBlockPattern = Pattern.compile("\"(dependencies|devDependencies)\"\\s*:\\s*\\{([\\s\\S]*?)\\}");
            Matcher blockMatcher = depsBlockPattern.matcher(content);

            while (blockMatcher.find()) {
                String dependenciesList = blockMatcher.group(2);
                Pattern depPattern = Pattern.compile("\"([^\"]+)\"\\s*:\\s*\"([^\"]+)\"");
                Matcher depMatcher = depPattern.matcher(dependenciesList);

                while (depMatcher.find()) {
                    String pkg = depMatcher.group(1).trim();
                    String ver = depMatcher.group(2).trim().replaceAll("[^0-9a-zA-Z.-]", ""); // strip ^, ~, >, etc.

                    for (VulnLib vuln : npmDb) {
                        if (vuln.name.equalsIgnoreCase(pkg) && vuln.isVulnerable(ver)) {
                            Vulnerability v = Vulnerability.builder()
                                    .scan(scan)
                                    .fileName(relativePath)
                                    .lineNumber(getLineOfSubstring(content, depMatcher.group(0)))
                                    .severity(vuln.severity)
                                    .ruleName(vuln.ruleName)
                                    .description("Vulnerable package " + pkg + "@" + ver + " found. " + vuln.description)
                                    .codeSnippet(depMatcher.group(0))
                                    .fixSuggestion(vuln.remediation)
                                    .build();
                            findings.add(v);
                        }
                    }
                }
            }
        } catch (IOException e) {
            // Ignore
        }
    }

    private int getLineOfSubstring(String fullText, String substring) {
        int index = fullText.indexOf(substring);
        if (index == -1) return 1;
        
        int line = 1;
        for (int i = 0; i < index; i++) {
            if (fullText.charAt(i) == '\n') {
                line++;
            }
        }
        return line;
    }
}
