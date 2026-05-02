package egovframework.example.chatbot.service;

import egovframework.example.chatbot.dto.ChatRoute;

public interface ChatRouterService {

    ChatRoute route(String question, String normalizedMessage, String locale);
}
