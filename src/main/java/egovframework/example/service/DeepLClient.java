package egovframework.example.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class DeepLClient {

    private static final Logger log = LoggerFactory.getLogger(DeepLClient.class);
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${deepl.base-url}")
    private String baseUrl; // https://api-free.deepl.com  (Free) / https://api.deepl.com (Pro)

    @Value("${deepl.api-key}")
    private String apiKey;

    public String translate(String text, String sourceLang, String targetLang, boolean enableBeta) {
        if (text == null || text.isBlank()) return text;
        if (apiKey == null || apiKey.isBlank() || baseUrl == null || baseUrl.isBlank()) {
            log.warn("DeepL disabled: missing apiKey/baseUrl");
            return null;
        }

        String url = baseUrl + "/v2/translate";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set("Authorization", "DeepL-Auth-Key " + apiKey);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("text", text);
        body.add("source_lang", sourceLang);  // "EN"
        body.add("target_lang", targetLang);  // "RU" or "TG"
        if (enableBeta) {
            body.add("enable_beta_languages", "1"); // For beta languages like TG
        }

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
        ResponseEntity<String> response;
        try {
            response = restTemplate.postForEntity(url, request, String.class);
        } catch (Exception e) {
            log.error("DeepL request failed: {}", e.getMessage());
            return null;
        }

        if (!response.getStatusCode().is2xxSuccessful()) {
            log.warn("DeepL non-2xx response: status={}", response.getStatusCode());
            return null;
        }

        if (response.getBody() == null || response.getBody().isBlank()) {
            log.warn("DeepL empty response body");
            return null;
        }

        // In practice, parse JSON and extract translations[0].text
        // For a minimal implementation, consider parsing with ObjectMapper in the service
        return response.getBody();
    }
    
    public String translateTextOnly(String text, String sourceLang, String targetLang, boolean enableBeta) {
        String raw = translate(text, sourceLang, targetLang, enableBeta);
        if (raw == null || raw.isBlank()) return null;

        try {
            JsonNode root = objectMapper.readTree(raw);
            JsonNode translations = root.get("translations");
            if (translations != null && translations.isArray() && translations.size() > 0) {
                JsonNode t = translations.get(0).get("text");
                return t != null ? t.asText() : null;
            }
        } catch (Exception e) {
            log.warn("DeepL parse failed: {}", e.getMessage());
            // If parsing fails, leave null for fallback
        }
        return null;
    }
}
