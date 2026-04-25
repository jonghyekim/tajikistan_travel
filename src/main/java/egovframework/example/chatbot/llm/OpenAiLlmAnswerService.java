package egovframework.example.chatbot.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import egovframework.example.chatbot.dto.GroundedAnswer;
import egovframework.example.chatbot.dto.IntentResult;
import egovframework.example.chatbot.rag.RagContext;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@ConditionalOnProperty(name = "chatbot.llm.mode", havingValue = "openai")
public class OpenAiLlmAnswerService implements LlmAnswerService {

    private static final String DEFAULT_RESPONSES_ENDPOINT = "https://api.openai.com/v1/responses";

    private final ChatbotLlmProperties properties;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public OpenAiLlmAnswerService(ChatbotLlmProperties properties,
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
    public GroundedAnswer generate(String question, IntentResult intent, RagContext context) {
        if (properties.getApiKey() == null || properties.getApiKey().isBlank()) {
            throw new IllegalStateException("chatbot.llm.api-key is required when chatbot.llm.mode=openai");
        }

        JsonNode response;
        try {
            response = restTemplate.postForObject(
                endpoint(),
                new HttpEntity<>(requestBody(question, intent, context), headers()),
                JsonNode.class
            );
        } catch (HttpStatusCodeException ex) {
            throw new IllegalStateException("OpenAI API failed: status=" + ex.getRawStatusCode() + ", body=" + ex.getResponseBodyAsString(), ex);
        }
        if (response == null) {
            throw new IllegalStateException("OpenAI response is empty");
        }

        JsonNode parsed = parseStructuredOutput(response);
        String answer = parsed.path("answer").asText("");
        List<String> sourceIds = readSourceIds(parsed.path("sourceIds"));
        return GroundedAnswer.llm(answer, sourceIds);
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

    private Map<String, Object> requestBody(String question, IntentResult intent, RagContext context) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", properties.getModel());
        body.put("instructions", systemPrompt(intent));
        body.put("input", List.of(
            Map.of(
                "role", "user",
                "content", userPrompt(question, intent, context)
            )
        ));
        body.put("text", Map.of(
            "format", Map.of(
                "type", "json_schema",
                "name", "grounded_answer",
                "strict", true,
                "schema", responseSchema()
            )
        ));
        return body;
    }

    private String systemPrompt(IntentResult intent) {
        return """
            You are a tourism chatbot response writer.
            Use only the provided database context.
            Do not add facts, phone numbers, addresses, hours, prices, or recommendations that are not in context.
            Respond in the requested locale: %s.
            Return only JSON matching the schema.
            sourceIds must be selected only from the allowed source IDs in context.
            If the context is insufficient, write a brief answer saying verified database information is unavailable and return an empty sourceIds array.
            """.formatted(intent.locale());
    }

    private String userPrompt(String question, IntentResult intent, RagContext context) {
        return """
            Question:
            %s

            Intent:
            %s

            Allowed source IDs:
            %s

            Database context:
            %s
            """.formatted(question, intent.intent(), context.sourceIds(), context.text());
    }

    private Map<String, Object> responseSchema() {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("additionalProperties", false);
        schema.put("required", List.of("answer", "sourceIds", "grounded"));
        schema.put("properties", Map.of(
            "answer", Map.of(
                "type", "string",
                "description", "Natural-language answer based only on the provided database context."
            ),
            "sourceIds", Map.of(
                "type", "array",
                "items", Map.of("type", "string"),
                "description", "Source IDs used to produce the answer. Must be a subset of allowed source IDs."
            ),
            "grounded", Map.of(
                "type", "boolean",
                "description", "True only when the answer is fully grounded in the provided database context."
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
        throw new IllegalStateException("OpenAI structured output text not found");
    }

    private JsonNode parseJson(String value) {
        try {
            return objectMapper.readTree(value);
        } catch (Exception ex) {
            throw new IllegalStateException("OpenAI structured output is not valid JSON", ex);
        }
    }

    private List<String> readSourceIds(JsonNode sourceIdsNode) {
        List<String> sourceIds = new ArrayList<>();
        if (!sourceIdsNode.isArray()) {
            return sourceIds;
        }
        for (JsonNode sourceId : sourceIdsNode) {
            if (sourceId.isTextual()) {
                sourceIds.add(sourceId.asText());
            }
        }
        return sourceIds;
    }
}
