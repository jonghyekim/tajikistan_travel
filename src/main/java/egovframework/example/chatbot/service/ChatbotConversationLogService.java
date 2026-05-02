package egovframework.example.chatbot.service;

import egovframework.example.chatbot.domain.AnswerType;
import egovframework.example.chatbot.domain.ChatbotConversationLog;
import egovframework.example.chatbot.dto.ChatResponse;
import egovframework.example.chatbot.dto.ChatRoute;
import egovframework.example.chatbot.repository.ChatbotConversationLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ChatbotConversationLogService {

    private static final Logger log = LoggerFactory.getLogger(ChatbotConversationLogService.class);

    private final ChatbotConversationLogRepository repository;

    public ChatbotConversationLogService(ChatbotConversationLogRepository repository) {
        this.repository = repository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(String message,
                       String normalizedMessage,
                       String locale,
                       ChatRoute route,
                       AnswerType answerType,
                       ChatResponse response,
                       long responseMs) {
        try {
            ChatbotConversationLog entry = new ChatbotConversationLog();
            entry.setMessage(truncate(message, 1000));
            entry.setNormalizedMessage(truncate(normalizedMessage, 1000));
            entry.setLocale(locale);
            entry.setIntent(response.intent());
            entry.setSearchPlan(route.searchPlan().name());
            entry.setKeyword(truncate(route.keyword(), 300));
            entry.setContactType(truncate(route.contactType(), 50));
            entry.setAnswerType(answerType == null ? null : answerType.name());
            entry.setSourceIds(truncate(join(response.sourceIds()), 1000));
            entry.setNoData(response.noData());
            entry.setGrounded(response.grounded());
            entry.setLlmUsed(response.llmUsed());
            entry.setAnswer(response.answer());
            entry.setResponseMs(responseMs);
            entry.setCreatedAt(LocalDateTime.now());
            repository.save(entry);
        } catch (RuntimeException ex) {
            log.warn("Chatbot conversation logging failed", ex);
        }
    }

    private String join(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "";
        }
        return String.join(",", values);
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
