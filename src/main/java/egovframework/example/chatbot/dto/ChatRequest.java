package egovframework.example.chatbot.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.io.Serializable;
import java.util.Map;

public record ChatRequest(
    @NotBlank @Size(max = 1000) String message,
    @Size(max = 20) String locale,
    String sessionId,
    Map<String, String> metadata
) implements Serializable {
}
