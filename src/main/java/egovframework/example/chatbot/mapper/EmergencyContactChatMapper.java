package egovframework.example.chatbot.mapper;

import egovframework.example.chatbot.dto.EmergencyContactFact;

import java.util.List;

public interface EmergencyContactChatMapper {

    List<EmergencyContactFact> findActiveContacts(String keyword, String locale);
}
