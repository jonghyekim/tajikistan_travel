package egovframework.example.chatbot.service;

import egovframework.example.chatbot.dto.OperatingHourFact;
import egovframework.example.chatbot.dto.TourPlaceFact;
import egovframework.example.chatbot.mapper.TourPlaceChatMapper;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TourPlaceQueryService {

    private static final int DEFAULT_LIMIT = 5;

    private final TourPlaceChatMapper tourPlaceChatMapper;

    public TourPlaceQueryService(TourPlaceChatMapper tourPlaceChatMapper) {
        this.tourPlaceChatMapper = tourPlaceChatMapper;
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "chatbot:places", key = "#keyword + ':' + #locale", unless = "#result.isEmpty()")
    public List<TourPlaceFact> searchPlaces(String keyword, String locale) {
        return tourPlaceChatMapper.searchPlaces(keyword, locale, DEFAULT_LIMIT);
    }

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "chatbot:operating-hours", key = "#keyword + ':' + #locale", unless = "#result.isEmpty()")
    public List<OperatingHourFact> findOperatingHours(String keyword, String locale) {
        return tourPlaceChatMapper.findOperatingHours(keyword, locale);
    }
}
