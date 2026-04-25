package egovframework.example.chatbot.controller;

import egovframework.example.chatbot.dto.ChatRequest;
import egovframework.example.chatbot.dto.ChatResponse;
import egovframework.example.chatbot.service.ChatApplicationService;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@Validated
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatApplicationService chatApplicationService;

    public ChatController(ChatApplicationService chatApplicationService) {
        this.chatApplicationService = chatApplicationService;
    }

    @PostMapping
    public ResponseEntity<ChatResponse> chat(@Valid @RequestBody ChatRequest request) {
        return ResponseEntity.ok(chatApplicationService.chat(request));
    }
}
