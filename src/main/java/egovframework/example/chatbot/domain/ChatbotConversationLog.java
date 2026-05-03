package egovframework.example.chatbot.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Lob;
import javax.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "chatbot_conversation_log",
    indexes = {
        @Index(name = "idx_chatbot_log_created_at", columnList = "created_at"),
        @Index(name = "idx_chatbot_log_intent", columnList = "intent"),
        @Index(name = "idx_chatbot_log_locale", columnList = "locale")
    }
)
public class ChatbotConversationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "log_id")
    private Long logId;

    @Column(name = "message", nullable = false, length = 1000)
    private String message;

    @Column(name = "normalized_message", length = 1000)
    private String normalizedMessage;

    @Column(name = "locale", nullable = false, length = 10)
    private String locale;

    @Column(name = "intent", nullable = false, length = 50)
    private String intent;

    @Column(name = "search_plan", nullable = false, length = 50)
    private String searchPlan;

    @Column(name = "keyword", length = 300)
    private String keyword;

    @Column(name = "contact_type", length = 50)
    private String contactType;

    @Column(name = "answer_type", length = 50)
    private String answerType;

    @Column(name = "source_ids", length = 1000)
    private String sourceIds;

    @Column(name = "no_data", nullable = false)
    private Boolean noData;

    @Column(name = "grounded", nullable = false)
    private Boolean grounded;

    @Column(name = "llm_used", nullable = false)
    private Boolean llmUsed;

    @Lob
    @Column(name = "answer")
    private String answer;

    @Column(name = "response_ms", nullable = false)
    private Long responseMs;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public Long getLogId() {
        return logId;
    }

    public String getMessage() {
        return message;
    }

    public String getNormalizedMessage() {
        return normalizedMessage;
    }

    public String getLocale() {
        return locale;
    }

    public String getIntent() {
        return intent;
    }

    public String getSearchPlan() {
        return searchPlan;
    }

    public String getKeyword() {
        return keyword;
    }

    public String getContactType() {
        return contactType;
    }

    public String getAnswerType() {
        return answerType;
    }

    public String getSourceIds() {
        return sourceIds;
    }

    public Boolean getNoData() {
        return noData;
    }

    public Boolean getGrounded() {
        return grounded;
    }

    public Boolean getLlmUsed() {
        return llmUsed;
    }

    public String getAnswer() {
        return answer;
    }

    public Long getResponseMs() {
        return responseMs;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setNormalizedMessage(String normalizedMessage) {
        this.normalizedMessage = normalizedMessage;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }

    public void setIntent(String intent) {
        this.intent = intent;
    }

    public void setSearchPlan(String searchPlan) {
        this.searchPlan = searchPlan;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public void setContactType(String contactType) {
        this.contactType = contactType;
    }

    public void setAnswerType(String answerType) {
        this.answerType = answerType;
    }

    public void setSourceIds(String sourceIds) {
        this.sourceIds = sourceIds;
    }

    public void setNoData(Boolean noData) {
        this.noData = noData;
    }

    public void setGrounded(Boolean grounded) {
        this.grounded = grounded;
    }

    public void setLlmUsed(Boolean llmUsed) {
        this.llmUsed = llmUsed;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public void setResponseMs(Long responseMs) {
        this.responseMs = responseMs;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
