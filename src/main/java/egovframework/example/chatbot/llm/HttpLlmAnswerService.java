package egovframework.example.chatbot.llm;

import egovframework.example.chatbot.dto.GroundedAnswer;
import egovframework.example.chatbot.dto.IntentResult;
import egovframework.example.chatbot.rag.RagContext;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Service
@ConditionalOnProperty(name = "chatbot.llm.mode", havingValue = "http")
public class HttpLlmAnswerService implements LlmAnswerService {

    private final ChatbotLlmProperties properties;
    private final RestTemplate restTemplate;

    public HttpLlmAnswerService(ChatbotLlmProperties properties, RestTemplateBuilder restTemplateBuilder) {
        this.properties = properties;
        this.restTemplate = restTemplateBuilder
            .setConnectTimeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
            .setReadTimeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
            .build();
    }

    @Override
    public GroundedAnswer generate(String question, IntentResult intent, RagContext context) {
        if (properties.getEndpoint() == null || properties.getEndpoint().isBlank()) {
            throw new IllegalStateException("chatbot.llm.endpoint is required when chatbot.llm.mode=http");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (properties.getApiKey() != null && !properties.getApiKey().isBlank()) {
            headers.setBearerAuth(properties.getApiKey());
        }

        Map<String, Object> request = Map.of(
            "question", question,
            "intent", intent.intent().name(),
            "instruction", "Answer only from context. Do not add facts outside context. Return cited sourceIds.",
            "context", context.text(),
            "allowedSourceIds", context.sourceIds()
        );

        HttpLlmResponse response = restTemplate.postForObject(
            properties.getEndpoint(),
            new HttpEntity<>(request, headers),
            HttpLlmResponse.class
        );
        if (response == null) {
            throw new IllegalStateException("LLM response is empty");
        }
        return GroundedAnswer.llm(response.answer(), response.sourceIds() == null ? List.of() : response.sourceIds());
    }

    public record HttpLlmResponse(
        String answer,
        List<String> sourceIds
    ) {
    }
}
