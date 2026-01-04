package egovframework.example.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class DeepLClient {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${deepl.base-url}")
    private String baseUrl; // https://api-free.deepl.com  (Free) / https://api.deepl.com (Pro)

    @Value("${deepl.api-key}")
    private String apiKey;

    public String translate(String text, String sourceLang, String targetLang, boolean enableBeta) {
        if (text == null || text.isBlank()) return text;

        String url = baseUrl + "/v2/translate";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set("Authorization", "DeepL-Auth-Key " + apiKey);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("text", text);
        body.add("source_lang", sourceLang);  // "EN"
        body.add("target_lang", targetLang);  // "RU" or "TG"
        if (enableBeta) {
            body.add("enable_beta_languages", "1"); // TG 같은 베타 언어용
        }

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

        // 실제로는 JSON 파싱해서 translations[0].text를 꺼내야 함
        // 최소 동작용으로는 ObjectMapper로 파싱하는 코드를 아래 서비스에서 같이 넣는 걸 추천
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
            // 파싱 실패하면 null로 두고 fallback 하게
        }
        return null;
    }
}
