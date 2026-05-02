package egovframework.example.chatbot.service;

import egovframework.example.chatbot.domain.ChatbotConversationLog;
import egovframework.example.chatbot.dto.ChatbotEvalCandidate;
import egovframework.example.chatbot.repository.ChatbotConversationLogRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ChatbotEvalCandidateService {

    private static final int DEFAULT_LIMIT = 50;
    private static final int MAX_LIMIT = 200;

    private final ChatbotConversationLogRepository repository;

    public ChatbotEvalCandidateService(ChatbotConversationLogRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<ChatbotEvalCandidate> findRecentCandidates(Integer limit) {
        int size = sanitizeLimit(limit);
        return repository.findEvaluationCandidates(PageRequest.of(0, size)).stream()
            .map(this::toCandidate)
            .toList();
    }

    private int sanitizeLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limit, MAX_LIMIT);
    }

    private ChatbotEvalCandidate toCandidate(ChatbotConversationLog log) {
        return new ChatbotEvalCandidate(
            log.getLogId(),
            log.getMessage(),
            log.getNormalizedMessage(),
            log.getLocale(),
            log.getIntent(),
            log.getSearchPlan(),
            log.getKeyword(),
            log.getContactType(),
            log.getAnswerType(),
            log.getSourceIds(),
            reason(log),
            log.getCreatedAt()
        );
    }

    private String reason(ChatbotConversationLog log) {
        if (Boolean.TRUE.equals(log.getNoData())) {
            return "NO_DATA_RESPONSE";
        }
        if (Boolean.FALSE.equals(log.getGrounded())) {
            return "UNGROUNDED_RESPONSE";
        }
        return "REVIEW";
    }
}
