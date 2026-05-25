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
public class SastEngine {

    private final List<Rule> rules = new ArrayList<>();

    public SastEngine() {
        // Java SQLi
        rules.add(new Rule(
                "JAVA_SQL_INJECTION",
                "CRITICAL",
                "Potential Java SQL Injection detected. Avoid building dynamic SQL queries using string concatenation.",
                "Use PreparedStatement instead: `PreparedStatement pstmt = conn.prepareStatement(\"SELECT * FROM users WHERE id = ?\"); pstmt.setInt(1, id);`",
                Pattern.compile("Statement\\s+[a-zA-Z0-9_]+\\s*=\\s*.*\\.createStatement.*|\\.executeQuery\\s*\\(\\s*[^\\\"]*?\\+\\s*[a-zA-Z0-9_]+|\\.executeUpdate\\s*\\(\\s*[^\\\"]*?\\+\\s*[a-zA-Z0-9_]+"),
                ".java"
        ));

        // Python SQLi
        rules.add(new Rule(
                "PYTHON_SQL_INJECTION",
                "CRITICAL",
                "Potential Python SQL Injection. Dynamic database queries can allow attackers to execute arbitrary SQL commands.",
                "Pass parameters as a tuple/list: `cursor.execute(\"SELECT * FROM users WHERE id = %s\", (user_id,))`",
                Pattern.compile("execute\\(\\s*f[\"'].*?\\{.*?\\}.*?[\"']\\)|execute\\(\\s*[\"'].*?%s.*?[\"']\\s*%|execute\\(\\s*[\"'].*?\\{\\}.*?[\"']\\.format"),
                ".py"
        ));

        // JS SQLi
        rules.add(new Rule(
                "JS_SQL_INJECTION",
                "CRITICAL",
                "Potential JavaScript/Node.js SQL Injection. Concatenating variables in raw DB queries exposes database contents.",
                "Use parameterized placeholders: `db.query('SELECT * FROM users WHERE id = ?', [userId])`",
                Pattern.compile("db\\.query\\(\\s*f?[\"'].*?\\$\\{.*?\\}.*?[\"']\\)|db\\.query\\(\\s*[\"'].*?\\+\\s*[a-zA-Z0-9_]+"),
                ".js", ".jsx", ".ts", ".tsx"
        ));

        // JS XSS
        rules.add(new Rule(
                "DOM_XSS",
                "HIGH",
                "Cross-Site Scripting (XSS) risk via unsafe DOM insertion (innerHTML/document.write).",
                "Use `textContent` or `innerText`, or run inputs through an HTML sanitization library (e.g. DOMPurify).",
                Pattern.compile("dangerouslySetInnerHTML|\\.innerHTML\\s*=|document\\.write\\("),
                ".js", ".jsx", ".ts", ".tsx"
        ));

        // Secrets Detection (General)
        rules.add(new Rule(
                "HARDCODED_SECRETS",
                "CRITICAL",
                "Hardcoded credential, AWS secret, or API Key detected in source file.",
                "Use system environment variables or standard vault solutions. Never commit credentials to git.",
                Pattern.compile("(?i)(aws_access_key_id|aws_secret_access_key|aws_key)[\s:='\"]+[A-Za-z0-9/+=]{20,40}|(?i)(secret|passwd|password|api_key|apikey|private_key|token)[\s:='\"]{1,3}[A-Za-z0-9-_/+=.]{16,60}"),
                ".java", ".py", ".js", ".ts", ".jsx", ".tsx"
        ));

        // Private Keys
        rules.add(new Rule(
                "PRIVATE_KEY_EXPOSURE",
                "CRITICAL",
                "Hardcoded RSA/EC/OpenSSH private key block detected.",
                "Move cryptographic keys out of code into encrypted credential backends.",
                Pattern.compile("-----BEGIN (RSA|EC|DSA|OPENSSH|PRIVATE) KEY-----"),
                ".java", ".py", ".js", ".ts", ".jsx", ".tsx", ".pem", ".key"
        ));

        // Java Weak Cryptography
        rules.add(new Rule(
                "JAVA_WEAK_CRYPTO",
                "HIGH",
                "Weak Cryptographic algorithms used (DES, Blowfish, RC4, MD5, SHA-1).",
                "Migrate to industry standards: `MessageDigest.getInstance(\"SHA-256\")` or `Cipher.getInstance(\"AES/GCM/NoPadding\")`.",
                Pattern.compile("Cipher\\.getInstance\\(\\s*[\"'](DES|Blowfish|RC4|ECB)[\"']|MessageDigest\\.getInstance\\(\\s*[\"'](MD5|SHA-1)[\"']"),
                ".java"
        ));

        // Java Unsafe Deserialization
        rules.add(new Rule(
                "JAVA_UNSAFE_DESERIALIZATION",
                "HIGH",
                "Unsafe Java Object Deserialization. Deserializing raw inputs can trigger RCE.",
                "Use JSON/Protobuf, or add an ObjectInputFilter class to restrict permitted objects.",
                Pattern.compile("ObjectInputStream\\s+[a-zA-Z0-9_]+\\s*=\\s*new\\s+ObjectInputStream|XMLDecoder"),
                ".java"
        ));

        // Python Unsafe Deserialization
        rules.add(new Rule(
                "PYTHON_UNSAFE_DESERIALIZATION",
                "HIGH",
                "Unsafe Python Deserialization. pickle.loads or yaml.load can run arbitrary commands.",
                "Replace pickle with standard JSON, and use `yaml.safe_load(data)` instead.",
                Pattern.compile("pickle\\.loads\\(|yaml\\.load\\(\\s*[a-zA-Z0-9_]+\\s*\\)(?!\\s*,\\s*Loader=)"),
                ".py"
        ));
    }

    public List<Vulnerability> scanDirectory(File rootDir, Scan scan) {
        List<Vulnerability> findings = new ArrayList<>();
        if (rootDir == null || !rootDir.exists()) {
            return findings;
        }
        scanPath(rootDir, rootDir, findings, scan);
        return findings;
    }

    private void scanPath(File rootDir, File currentFile, List<Vulnerability> findings, Scan scan) {
        String name = currentFile.getName();
        if (currentFile.isDirectory()) {
            // Ignore build/cache folders
            if (name.equals(".git") || name.equals("node_modules") || name.equals("target")
                    || name.equals("bin") || name.equals(".idea") || name.equals("build")
                    || name.equals("dist") || name.equals(".gradle")) {
                return;
            }
            File[] children = currentFile.listFiles();
            if (children != null) {
                for (File child : children) {
                    scanPath(rootDir, child, findings, scan);
                }
            }
        } else {
            scanFile(rootDir, currentFile, findings, scan);
        }
    }

    private void scanFile(File rootDir, File file, List<Vulnerability> findings, Scan scan) {
        String fileName = file.getName().toLowerCase();
        List<Rule> applicableRules = new ArrayList<>();
        for (Rule rule : rules) {
            if (rule.appliesTo(fileName)) {
                applicableRules.add(rule);
            }
        }

        if (applicableRules.isEmpty()) {
            return;
        }

        try {
            List<String> lines = Files.readAllLines(file.toPath());
            String relativePath = rootDir.toPath().relativize(file.toPath()).toString().replace('\\', '/');

            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                for (Rule rule : applicableRules) {
                    Matcher matcher = rule.pattern.matcher(line);
                    if (matcher.find()) {
                        // Gather context snippet (5 lines: line-2 to line+2)
                        String snippet = getContextSnippet(lines, i);

                        Vulnerability vuln = Vulnerability.builder()
                                .scan(scan)
                                .fileName(relativePath)
                                .lineNumber(i + 1)
                                .severity(rule.severity)
                                .ruleName(rule.name)
                                .description(rule.description)
                                .codeSnippet(snippet)
                                .fixSuggestion(rule.remediation)
                                .build();

                        findings.add(vuln);
                    }
                }
            }
        } catch (IOException e) {
            // Ignore binary or unreadable files
        }
    }

    private String getContextSnippet(List<String> lines, int targetIndex) {
        int start = Math.max(0, targetIndex - 2);
        int end = Math.min(lines.size() - 1, targetIndex + 2);
        StringBuilder sb = new StringBuilder();
        for (int i = start; i <= end; i++) {
            sb.append(i + 1).append(": ").append(lines.get(i));
            if (i < end) {
                sb.append("\n");
            }
        }
        return sb.toString();
    }

    private static class Rule {
        String name;
        String severity;
        String description;
        String remediation;
        Pattern pattern;
        String[] extensions;

        Rule(String name, String severity, String description, String remediation, Pattern pattern, String... extensions) {
            this.name = name;
            this.severity = severity;
            this.description = description;
            this.remediation = remediation;
            this.pattern = pattern;
            this.extensions = extensions;
        }

        boolean appliesTo(String fileName) {
            for (String ext : extensions) {
                if (fileName.endsWith(ext)) {
                    return true;
                }
            }
            return false;
        }
    }
}
