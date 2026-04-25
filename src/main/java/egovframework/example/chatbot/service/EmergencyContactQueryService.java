package egovframework.example.chatbot.service;

import egovframework.example.chatbot.dto.EmergencyContactFact;
import egovframework.example.chatbot.mapper.EmergencyContactChatMapper;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class EmergencyContactQueryService {

    private final EmergencyContactChatMapper emergencyContactChatMapper;

    public EmergencyContactQueryService(EmergencyContactChatMapper emergencyContactChatMapper) {
        this.emergencyContactChatMapper = emergencyContactChatMapper;
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "chatbot:emergency-contacts", key = "#keyword + ':' + #locale", unless = "#result.isEmpty()")
    public List<EmergencyContactFact> findActiveContacts(String keyword, String locale) {
        return emergencyContactChatMapper.findActiveContacts(keyword, locale);
    }
}
