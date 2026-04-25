package egovframework.example.chatbot.mapper;

import egovframework.example.chatbot.dto.OperatingHourFact;
import egovframework.example.chatbot.dto.TourPlaceFact;

import java.util.List;
import java.util.Optional;

public interface TourPlaceChatMapper {

    List<TourPlaceFact> searchPlaces(String keyword, String locale, int limit);

    Optional<TourPlaceFact> findPlaceByKeyword(String keyword, String locale);

    List<OperatingHourFact> findOperatingHours(String keyword, String locale);
}
