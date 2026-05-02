package egovframework.example.chatbot.service;

import egovframework.example.chatbot.domain.ChatbotConversationLog;
import egovframework.example.chatbot.dto.ChatbotEvalCandidate;
import egovframework.example.chatbot.repository.ChatbotConversationLogRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChatbotEvalCandidateServiceTest {

    private final ChatbotConversationLogRepository repository = mock(ChatbotConversationLogRepository.class);
    private final ChatbotEvalCandidateService service = new ChatbotEvalCandidateService(repository);

    @Test
    void returnsNoDataAndUngroundedLogsAsEvalCandidates() {
        ChatbotConversationLog noDataLog = log("없는장소 위치", true, true);
        ChatbotConversationLog ungroundedLog = log("루다키 공원 가격", false, false);
        when(repository.findEvaluationCandidates(any(Pageable.class)))
            .thenReturn(List.of(noDataLog, ungroundedLog));

        List<ChatbotEvalCandidate> candidates = service.findRecentCandidates(10);

        assertThat(candidates).hasSize(2);
        assertThat(candidates).extracting(ChatbotEvalCandidate::reason)
            .containsExactly("NO_DATA_RESPONSE", "UNGROUNDED_RESPONSE");
        assertThat(candidates).extracting(ChatbotEvalCandidate::message)
            .containsExactly("없는장소 위치", "루다키 공원 가격");
        verify(repository).findEvaluationCandidates(any(Pageable.class));
    }

    private ChatbotConversationLog log(String message, boolean noData, boolean grounded) {
        ChatbotConversationLog log = new ChatbotConversationLog();
        log.setMessage(message);
        log.setNormalizedMessage(message);
        log.setLocale("ko");
        log.setIntent("TOUR_PLACE_SEARCH");
        log.setSearchPlan("PLACE_BY_NAME");
        log.setKeyword(message);
        log.setAnswerType(noData ? "NO_DATA" : "OPEN_DETAIL_PAGE");
        log.setSourceIds("");
        log.setNoData(noData);
        log.setGrounded(grounded);
        log.setLlmUsed(false);
        log.setAnswer("");
        log.setResponseMs(3L);
        log.setCreatedAt(LocalDateTime.now());
        return log;
    }
}
