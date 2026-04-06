package egovframework.example.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import egovframework.example.domain.CategoryCodeI18n;
import egovframework.example.domain.Image;
import egovframework.example.domain.Member;
import egovframework.example.domain.MemberCalendar;
import egovframework.example.domain.RegionCodeI18n;
import egovframework.example.domain.TourPlace;
import egovframework.example.domain.TourPlaceI18n;
import egovframework.example.dto.calendar.CalendarPlaceResponseDto;
import egovframework.example.dto.calendar.CalendarRequestDto;
import egovframework.example.repository.MemberCalendarRepository;
import egovframework.example.repository.MemberRepository;
import egovframework.example.repository.TourPlaceRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class MemberCalendarService {

    private final MemberCalendarRepository memberCalendarRepository;
    private final MemberRepository memberRepository;
    private final TourPlaceRepository tourPlaceRepository;

    public void addCalendar(Long memberId, CalendarRequestDto requestDto) {
        if (memberCalendarRepository.existsByMember_IdAndPlace_PlaceIdAndStartDate(
                memberId,
                requestDto.getPlaceId(),
                requestDto.getStartDate())) {
            return;
        }

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        TourPlace place = tourPlaceRepository.findById(requestDto.getPlaceId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 관광지입니다."));

        MemberCalendar calendar = new MemberCalendar();
        calendar.setMember(member);
        calendar.setPlace(place);
        calendar.setStartDate(requestDto.getStartDate());

        memberCalendarRepository.save(calendar);
    }

    public void removeCalendar(Long memberId, Long placeId, LocalDate startDate) {
        MemberCalendar calendar = memberCalendarRepository
                .findByMember_IdAndPlace_PlaceIdAndStartDate(memberId, placeId, startDate)
                .orElse(null);

        if (calendar != null) {
            memberCalendarRepository.delete(calendar);
        }
    }

    public void removeCalendarById(Long memberId, Long calendarId) {
        memberCalendarRepository.deleteByIdAndMember_Id(calendarId, memberId);
    }

    @Transactional(readOnly = true)
    public List<CalendarPlaceResponseDto> getMyCalendars(Long memberId, String lang) {
        List<MemberCalendar> calendars = memberCalendarRepository.findAllByMember_Id(memberId);
        List<CalendarPlaceResponseDto> result = new ArrayList<>();

        for (MemberCalendar calendar : calendars) {
            TourPlace place = calendar.getPlace();

            TourPlaceI18n placeI18n = findPlaceI18n(place, lang);
            String title = placeI18n != null ? placeI18n.getTitle() : "";
            String content = placeI18n != null ? placeI18n.getContent() : "";

            String imageUrl = "";
            if (place.getImages() != null && !place.getImages().isEmpty()) {
                Image image = place.getImages().get(0);
                imageUrl = image.getFileUrl();
            }

            String categoryName = findCategoryName(place, lang);
            String regionName = findRegionName(place, lang);

            result.add(new CalendarPlaceResponseDto(
                    calendar.getId(),
                    place.getPlaceId(),
                    title,
                    content,
                    imageUrl,
                    categoryName,
                    regionName,
                    calendar.getStartDate()
            ));
        }

        return result;
    }

    @Transactional(readOnly = true)
    public List<CalendarPlaceResponseDto> getMyCalendarsByDate(Long memberId, LocalDate startDate, String lang) {
        List<MemberCalendar> calendars = memberCalendarRepository.findAllByMember_IdAndStartDate(memberId, startDate);
        List<CalendarPlaceResponseDto> result = new ArrayList<>();

        for (MemberCalendar calendar : calendars) {
            TourPlace place = calendar.getPlace();

            TourPlaceI18n placeI18n = findPlaceI18n(place, lang);
            String title = placeI18n != null ? placeI18n.getTitle() : "";
            String content = placeI18n != null ? placeI18n.getContent() : "";

            String imageUrl = "";
            if (place.getImages() != null && !place.getImages().isEmpty()) {
                Image image = place.getImages().get(0);
                imageUrl = image.getFileUrl();
            }

            String categoryName = findCategoryName(place, lang);
            String regionName = findRegionName(place, lang);

            result.add(new CalendarPlaceResponseDto(
                    calendar.getId(),
                    place.getPlaceId(),
                    title,
                    content,
                    imageUrl,
                    categoryName,
                    regionName,
                    calendar.getStartDate()
            ));
        }

        return result;
    }

    private TourPlaceI18n findPlaceI18n(TourPlace place, String lang) {
        if (place.getI18ns() == null || place.getI18ns().isEmpty()) {
            return null;
        }

        for (TourPlaceI18n i18n : place.getI18ns()) {
            if (lang.equalsIgnoreCase(i18n.getLocale())) {
                return i18n;
            }
        }

        return place.getI18ns().get(0);
    }

    private String findCategoryName(TourPlace place, String lang) {
        if (place.getCategory() == null || place.getCategory().getI18ns() == null || place.getCategory().getI18ns().isEmpty()) {
            return "";
        }

        for (CategoryCodeI18n i18n : place.getCategory().getI18ns()) {
            if (lang.equalsIgnoreCase(i18n.getLocale())) {
                return i18n.getName();
            }
        }

        return place.getCategory().getI18ns().get(0).getName();
    }

    private String findRegionName(TourPlace place, String lang) {
        if (place.getRegion() == null || place.getRegion().getI18ns() == null || place.getRegion().getI18ns().isEmpty()) {
            return "";
        }

        for (RegionCodeI18n i18n : place.getRegion().getI18ns()) {
            if (lang.equalsIgnoreCase(i18n.getLocale())) {
                return i18n.getName();
            }
        }

        return place.getRegion().getI18ns().get(0).getName();
    }

    @Transactional(readOnly = true)
    public List<Long> getMyCalendarPlaceIds(Long memberId) {
        List<MemberCalendar> calendars = memberCalendarRepository.findAllByMember_Id(memberId);
        List<Long> result = new ArrayList<>();

        for (MemberCalendar calendar : calendars) {
            result.add(calendar.getPlace().getPlaceId());
        }

        return result;
    }
}