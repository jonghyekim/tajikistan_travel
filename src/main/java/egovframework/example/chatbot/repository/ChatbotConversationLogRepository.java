package egovframework.example.chatbot.repository;

import egovframework.example.chatbot.domain.ChatbotConversationLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ChatbotConversationLogRepository extends JpaRepository<ChatbotConversationLog, Long> {

    @Query("""
        select log
        from ChatbotConversationLog log
        where log.noData = true
           or log.grounded = false
        order by log.createdAt desc
        """)
    List<ChatbotConversationLog> findEvaluationCandidates(Pageable pageable);
}
