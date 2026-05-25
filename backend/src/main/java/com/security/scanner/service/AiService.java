package com.security.scanner.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Service
public class AiService {

    @Value("${app.gemini.api-key:}")
    private String apiKey;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public static class FixSuggestionResponse {
        public String secureCode;
        public String explanation;

        public FixSuggestionResponse(String secureCode, String explanation) {
            this.secureCode = secureCode;
            this.explanation = explanation;
        }
    }

    public FixSuggestionResponse generateFix(String ruleName, String severity, String description, String codeSnippet) {
        if (apiKey != null && !apiKey.trim().isEmpty()) {
            try {
                return callGemini(ruleName, severity, description, codeSnippet);
            } catch (Exception e) {
                System.err.println("Gemini API call failed, falling back to local database: " + e.getMessage());
            }
        }
        return getLocalFallbackFix(ruleName, codeSnippet);
    }

    private FixSuggestionResponse callGemini(String ruleName, String severity, String description, String codeSnippet) throws Exception {
        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + apiKey;

        String prompt = String.format(
                "You are an enterprise secure coding expert. Please analyze this vulnerability and suggest a secure fix.\n" +
                "Vulnerability Type: %s\n" +
                "Severity: %s\n" +
                "Description: %s\n" +
                "Vulnerable Code:\n%s\n\n" +
                "Provide your response EXACTLY as a raw JSON object matching the following schema. Do NOT wrap it in ```json blocks or any markdown:\n" +
                "{\n" +
                "  \"secureCode\": \"your secure refactored code block here\",\n" +
                "  \"explanation\": \"your secure coding explanation here\"\n" +
                "}",
                ruleName, severity, description, codeSnippet
        );

        Map<String, Object> textPart = Map.of("text", prompt);
        Map<String, Object> parts = Map.of("parts", java.util.List.of(textPart));
        Map<String, Object> contents = Map.of("contents", java.util.List.of(parts));
        String requestBody = objectMapper.writeValueAsString(contents);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            JsonNode root = objectMapper.readTree(response.body());
            String rawJsonText = root.path("candidates")
                    .path(0)
                    .path("content")
                    .path("parts")
                    .path(0)
                    .path("text")
                    .asText()
                    .trim();

            // Strip potential markdown JSON code block wrap
            if (rawJsonText.startsWith("```")) {
                rawJsonText = rawJsonText.substring(rawJsonText.indexOf("{"));
                if (rawJsonText.endsWith("```")) {
                    rawJsonText = rawJsonText.substring(0, rawJsonText.lastIndexOf("`") - 2);
                }
            }

            JsonNode fixNode = objectMapper.readTree(rawJsonText);
            return new FixSuggestionResponse(
                    fixNode.path("secureCode").asText(),
                    fixNode.path("explanation").asText()
            );
        } else {
            throw new RuntimeException("API error: Status code " + response.statusCode() + ", body: " + response.body());
        }
    }

    private FixSuggestionResponse getLocalFallbackFix(String ruleName, String codeSnippet) {
        String cleanSnippet = stripLineNumbers(codeSnippet);
        switch (ruleName.toUpperCase()) {
            case "JAVA_SQL_INJECTION":
                return new FixSuggestionResponse(
                        "// Use PreparedStatements with parameterized inputs\n" +
                        "String sql = \"SELECT * FROM users WHERE email = ? AND status = ?\";\n" +
                        "try (PreparedStatement pstmt = conn.prepareStatement(sql)) {\n" +
                        "    pstmt.setString(1, emailInput);\n" +
                        "    pstmt.setString(2, \"ACTIVE\");\n" +
                        "    try (ResultSet rs = pstmt.executeQuery()) {\n" +
                        "        // process result set safely\n" +
                        "    }\n" +
                        "}",
                        "Concatenating user inputs directly into raw SQL strings allows SQL injection. Utilizing PreparedStatement ensures parameters are safely escaped by the SQL driver."
                );

            case "PYTHON_SQL_INJECTION":
                return new FixSuggestionResponse(
                        "# Use parameterized placeholder parameters (tuple/dict)\n" +
                        "query = \"SELECT * FROM users WHERE username = %s AND role = %s\"\n" +
                        "cursor.execute(query, (user_input, 'DEVELOPER'))",
                        "Avoid string format (%) or f-strings inside raw SQL. SQL parameters should be passed separately as a tuple, letting the database driver parameterize the inputs safely."
                );

            case "JS_SQL_INJECTION":
                return new FixSuggestionResponse(
                        "// Use parameterized queries with bindings array\n" +
                        "const sql = 'SELECT * FROM users WHERE id = ? AND role = ?';\n" +
                        "const [rows] = await db.execute(sql, [userIdInput, 'USER']);",
                        "Binding placeholders (?) dynamically sanitize parameters and separate instruction logic from inputs, neutralizing string-injection attacks."
                );

            case "DOM_XSS":
                return new FixSuggestionResponse(
                        "// Use safe DOM assignments or HTML sanitization libraries\n" +
                        "const safeText = document.createTextNode(userInput);\n" +
                        "element.appendChild(safeText);\n" +
                        "// Or if HTML rendering is required: \n" +
                        "// element.innerHTML = DOMPurify.sanitize(userInput);",
                        "Assigning unvalidated user input directly to innerHTML bypasses DOM defenses. Use textContent to bind plain text, or filter raw inputs using DOMPurify."
                );

            case "HARDCODED_SECRETS":
                return new FixSuggestionResponse(
                        "// Load configuration parameters dynamically from environment configs\n" +
                        "String dbPassword = System.getenv(\"DB_PASSWORD\");\n" +
                        "// Or read securely using Configuration Properties \n" +
                        "@Value(\"${database.password}\")\n" +
                        "private String databasePassword;",
                        "Do not commit API tokens, passwords, or secret keys to source control repositories. Extract credentials into environment variables or secrets managers."
                );

            case "PRIVATE_KEY_EXPOSURE":
                return new FixSuggestionResponse(
                        "# Load private key from an external vault, configuration path, or env variable\n" +
                        "import os\n" +
                        "private_key_path = os.environ.get('JWT_PRIVATE_KEY_PATH')\n" +
                        "with open(private_key_path, 'r') as key_file:\n" +
                        "    private_key = key_file.read()",
                        "Sensitive key blocks should remain on protected host directories or vault vaults, and referenced at runtime by filepath environment variables."
                );

            case "JAVA_WEAK_CRYPTO":
                return new FixSuggestionResponse(
                        "// Upgrade hash algorithms to SHA-256/SHA-512 and symmetric keys to AES/GCM\n" +
                        "MessageDigest digest = MessageDigest.getInstance(\"SHA-256\");\n" +
                        "Cipher aesCipher = Cipher.getInstance(\"AES/GCM/NoPadding\");",
                        "Old standards like MD5, SHA-1, and DES suffer from collision vulnerabilities or key-exhaustion limits. Upgrade cryptographic providers to SHA-256 and AES/GCM."
                );

            case "JAVA_UNSAFE_DESERIALIZATION":
                return new FixSuggestionResponse(
                        "// Replace native Java serialization with clean format parsers like JSON or Protobuf\n" +
                        "ObjectMapper mapper = new ObjectMapper();\n" +
                        "User user = mapper.readValue(jsonString, User.class);",
                        "Deserializing untrusted ObjectInputStreams allows arbitrary class instantiations, leading to Remote Code Execution. Use text-based serialization formats (JSON, XML schemas, or Protobuf)."
                );

            case "PYTHON_UNSAFE_DESERIALIZATION":
                return new FixSuggestionResponse(
                        "# Parse structured files using safe loading configurations\n" +
                        "import yaml\n" +
                        "data = yaml.safe_load(yaml_string)\n" +
                        "# Avoid using pickle for untrusted network payloads",
                        "Standard pickle.loads() and yaml.load() are capable of executing arbitrary commands embedded inside files. Ensure you compile configs using yaml.safe_load()."
                );

            default:
                return new FixSuggestionResponse(
                        "// Remediation for: " + ruleName + "\n" +
                        "// Please verify this secure pattern against your implementation:\n" +
                        cleanSnippet,
                        "Verify input filters, apply context-aware escaping, compile dependencies with updated security patches, and avoid passing dynamic strings to runtime execution engines."
                );
        }
    }

    private String stripLineNumbers(String codeSnippet) {
        if (codeSnippet == null) return "";
        StringBuilder sb = new StringBuilder();
        String[] lines = codeSnippet.split("\n");
        for (String line : lines) {
            // Strip the leading "L: " line number format from the SastEngine context
            String clean = line.replaceFirst("^\\d+:\\s", "");
            sb.append(clean).append("\n");
        }
        return sb.toString().trim();
    }
}
