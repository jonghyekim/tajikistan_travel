package egovframework.example.chatbot.llm;

import egovframework.example.chatbot.dto.GroundedAnswer;
import egovframework.example.chatbot.dto.IntentResult;
import egovframework.example.chatbot.rag.RagContext;

public interface LlmAnswerService {

    GroundedAnswer generate(String question, IntentResult intent, RagContext context);
}
