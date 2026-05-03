package egovframework.example.chatbot.service;

import egovframework.example.chatbot.domain.AnswerType;
import egovframework.example.chatbot.domain.ChatIntent;
import egovframework.example.chatbot.domain.ChatbotConversationLog;
import egovframework.example.chatbot.domain.SearchPlan;
import egovframework.example.chatbot.dto.ChatResponse;
import egovframework.example.chatbot.dto.ChatRoute;
import egovframework.example.chatbot.repository.ChatbotConversationLogRepository;
import org.junit.jupiter.api.Test;

import javax.persistence.Table;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChatbotConversationLogServiceTest {

    private final ChatbotConversationLogRepository repository = mock(ChatbotConversationLogRepository.class);
    private final ChatbotConversationLogService service = new ChatbotConversationLogService(repository);

    @Test
    void conversationLogEntityUsesExistingMysqlTableName() {
        Table table = ChatbotConversationLog.class.getAnnotation(Table.class);

        org.assertj.core.api.Assertions.assertThat(table.name()).isEqualTo("chatbot_conversation_log");
    }

    @Test
    void savesConversationLog() {
        ChatRoute route = new ChatRoute(
            ChatIntent.EMERGENCY_CONTACT,
            SearchPlan.ALL_EMERGENCY_CONTACTS,
            null,
            null,
            0.9
        );
        ChatResponse response = new ChatResponse(
            "Police: 102",
            ChatIntent.EMERGENCY_CONTACT.name(),
            true,
            false,
            false,
            List.of("emergency_contact:2"),
            List.of()
        );

        service.record(
            "긴급상황. 연락처 필요",
            "긴급상황 연락처 필요",
            "ko",
            route,
            AnswerType.EMERGENCY_CONTACTS,
            response,
            12L
        );

        verify(repository).save(any(ChatbotConversationLog.class));
    }

    @Test
    void doesNotFailChatResponseWhenLoggingFails() {
        when(repository.save(any(ChatbotConversationLog.class)))
            .thenThrow(new IllegalStateException("database unavailable"));
        ChatRoute route = new ChatRoute(
            ChatIntent.UNKNOWN,
            SearchPlan.NONE,
            null,
            null,
            0.0
        );
        ChatResponse response = ChatResponse.noData(ChatIntent.UNKNOWN.name(), "ko");

        assertThatCode(() -> service.record(
            "???",
            "???",
            "ko",
            route,
            AnswerType.NO_DATA,
            response,
            1L
        )).doesNotThrowAnyException();
    }
}
