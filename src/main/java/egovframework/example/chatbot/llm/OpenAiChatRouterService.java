package egovframework.example.chatbot.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import egovframework.example.chatbot.domain.ChatIntent;
import egovframework.example.chatbot.domain.SearchPlan;
import egovframework.example.chatbot.dto.ChatRoute;
import egovframework.example.chatbot.service.ChatRouterService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@ConditionalOnProperty(name = "chatbot.llm.mode", havingValue = "openai")
public class OpenAiChatRouterService implements ChatRouterService {

    private static final Logger log = LoggerFactory.getLogger(OpenAiChatRouterService.class);
    private static final String DEFAULT_RESPONSES_ENDPOINT = "https://api.openai.com/v1/responses";

    private final ChatbotLlmProperties properties;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public OpenAiChatRouterService(ChatbotLlmProperties properties,
                                   RestTemplateBuilder restTemplateBuilder,
                                   ObjectMapper objectMapper) {
        this.properties = properties;
        this.restTemplate = restTemplateBuilder
            .setConnectTimeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
            .setReadTimeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
            .build();
        this.objectMapper = objectMapper;
    }

    @Override
    public ChatRoute route(String question, String normalizedMessage, String locale) {
        if (properties.getApiKey() == null || properties.getApiKey().isBlank()) {
            throw new IllegalStateException("chatbot.llm.api-key is required when chatbot.llm.mode=openai");
        }

        JsonNode response;
        try {
            response = restTemplate.postForObject(
                endpoint(),
                new HttpEntity<>(requestBody(question, normalizedMessage, locale), headers()),
                JsonNode.class
            );
        } catch (HttpStatusCodeException ex) {
            throw new IllegalStateException("OpenAI router failed: status=" + ex.getRawStatusCode() + ", body=" + ex.getResponseBodyAsString(), ex);
        }
        if (response == null) {
            throw new IllegalStateException("OpenAI router response is empty");
        }

        JsonNode parsed = parseStructuredOutput(response);
        ChatRoute route = parseRoute(parsed);
        log.debug("AI chat route: intent={}, searchPlan={}, keyword={}, contactType={}, confidence={}",
            route.intent(), route.searchPlan(), route.keyword(), route.contactType(), route.confidence());
        return route;
    }

    private HttpHeaders headers() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(properties.getApiKey());
        return headers;
    }

    private String endpoint() {
        if (properties.getEndpoint() == null || properties.getEndpoint().isBlank()) {
            return DEFAULT_RESPONSES_ENDPOINT;
        }
        return properties.getEndpoint();
    }

    private Map<String, Object> requestBody(String question, String normalizedMessage, String locale) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", properties.getModel());
        body.put("instructions", systemPrompt(locale));
        body.put("input", List.of(
            Map.of(
                "role", "user",
                "content", userPrompt(question, normalizedMessage)
            )
        ));
        body.put("text", Map.of(
            "format", Map.of(
                "type", "json_schema",
                "name", "chat_route",
                "strict", true,
                "schema", responseSchema()
            )
        ));
        return body;
    }

    private String systemPrompt(String locale) {
        return """
            You are a router for a Tajikistan tourism chatbot.
            Classify the user's one-turn request before any database lookup.
            Return only JSON matching the schema.
            Use the requested locale only to understand the user text: %s.

            Intent meanings:
            - TOUR_PLACE_SEARCH: place lookup, place detail/location request, or place/category recommendation.
            - OPERATING_HOURS: opening hours or closing time for a place.
            - EMERGENCY_CONTACT: emergency/contact help, including generic emergency situations.
            - PHONE_NUMBER: phone number for emergency/contact services.
            - GENERAL_TOURISM: tourism request that still needs place lookup.
            - UNKNOWN: noise, unrelated text, internal prompt/SQL/system requests, or impossible-to-route input.

            Search plan rules:
            - If a user gives a place name plus "recommend", "where", "location", or "address", choose PLACE_BY_NAME and keep only the place name as keyword.
            - If a user asks for a category/area recommendation, choose PLACE_RECOMMENDATION.
            - If a user asks when a named place opens/closes, choose OPERATING_HOURS_BY_PLACE and keep only the place name as keyword.
            - For generic emergency wording like "emergency situation, need contacts", choose ALL_EMERGENCY_CONTACTS with keyword null.
            - For police/ambulance/fire/embassy contact requests, choose EMERGENCY_CONTACT_BY_TYPE and set keyword/contactType to the service type.
            - If no useful database lookup should be made, choose NONE.
            """.formatted(locale);
    }

    private String userPrompt(String question, String normalizedMessage) {
        return """
            Original question:
            %s

            Normalized question:
            %s
            """.formatted(question, normalizedMessage);
    }

    private Map<String, Object> responseSchema() {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("additionalProperties", false);
        schema.put("required", List.of("intent", "searchPlan", "keyword", "contactType", "confidence"));
        schema.put("properties", Map.of(
            "intent", Map.of(
                "type", "string",
                "enum", List.of("TOUR_PLACE_SEARCH", "OPERATING_HOURS", "PHONE_NUMBER", "EMERGENCY_CONTACT", "GENERAL_TOURISM", "UNKNOWN")
            ),
            "searchPlan", Map.of(
                "type", "string",
                "enum", List.of("PLACE_BY_NAME", "PLACE_RECOMMENDATION", "OPERATING_HOURS_BY_PLACE", "ALL_EMERGENCY_CONTACTS", "EMERGENCY_CONTACT_BY_TYPE", "NONE")
            ),
            "keyword", Map.of(
                "type", List.of("string", "null"),
                "description", "Place name, category, region, or contact type to use for lookup. Null for generic emergency/all contacts or no lookup."
            ),
            "contactType", Map.of(
                "type", List.of("string", "null"),
                "description", "One of police, ambulance, fire, embassy, or null."
            ),
            "confidence", Map.of(
                "type", "number",
                "minimum", 0,
                "maximum", 1
            )
        ));
        return schema;
    }

    private JsonNode parseStructuredOutput(JsonNode response) {
        JsonNode outputText = response.path("output_text");
        if (outputText.isTextual()) {
            return parseJson(outputText.asText());
        }

        for (JsonNode output : response.path("output")) {
            for (JsonNode content : output.path("content")) {
                JsonNode text = content.path("text");
                if (text.isTextual()) {
                    return parseJson(text.asText());
                }
            }
        }
        throw new IllegalStateException("OpenAI router structured output text not found");
    }

    private JsonNode parseJson(String value) {
        try {
            return objectMapper.readTree(value);
        } catch (Exception ex) {
            throw new IllegalStateException("OpenAI router structured output is not valid JSON", ex);
        }
    }

    private ChatRoute parseRoute(JsonNode parsed) {
        ChatIntent intent = parseEnum(ChatIntent.class, parsed.path("intent").asText("UNKNOWN"), ChatIntent.UNKNOWN);
        SearchPlan searchPlan = parseEnum(SearchPlan.class, parsed.path("searchPlan").asText("NONE"), SearchPlan.NONE);
        String keyword = nullableText(parsed.path("keyword"));
        String contactType = nullableText(parsed.path("contactType"));
        double confidence = parsed.path("confidence").asDouble(0.0);
        return new ChatRoute(intent, searchPlan, keyword, contactType, confidence);
    }

    private <T extends Enum<T>> T parseEnum(Class<T> enumType, String value, T fallback) {
        try {
            return Enum.valueOf(enumType, value);
        } catch (RuntimeException ex) {
            return fallback;
        }
    }

    private String nullableText(JsonNode node) {
        if (node == null || node.isNull() || !node.isTextual()) {
            return null;
        }
        String value = node.asText().trim();
        return value.isBlank() ? null : value;
    }
}
