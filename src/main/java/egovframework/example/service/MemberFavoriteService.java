package egovframework.example.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import egovframework.example.domain.CategoryCodeI18n;
import egovframework.example.domain.Image;
import egovframework.example.domain.Member;
import egovframework.example.domain.MemberFavorite;
import egovframework.example.domain.RegionCodeI18n;
import egovframework.example.domain.TourPlace;
import egovframework.example.domain.TourPlaceI18n;
import egovframework.example.dto.favorite.FavoritePlaceResponseDto;
import egovframework.example.repository.MemberFavoriteRepository;
import egovframework.example.repository.MemberRepository;
import egovframework.example.repository.TourPlaceRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class MemberFavoriteService {

    private final MemberFavoriteRepository memberFavoriteRepository;
    private final MemberRepository memberRepository;
    private final TourPlaceRepository tourPlaceRepository;

    public void addFavorite(Long memberId, Long placeId) {
        if (memberFavoriteRepository.existsByMember_IdAndPlace_PlaceId(memberId, placeId)) {
            return;
        }

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        TourPlace place = tourPlaceRepository.findById(placeId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 관광지입니다."));

        MemberFavorite favorite = new MemberFavorite();
        favorite.setMember(member);
        favorite.setPlace(place);

        memberFavoriteRepository.save(favorite);
    }

    public void removeFavorite(Long memberId, Long placeId) {
        MemberFavorite favorite = memberFavoriteRepository
                .findByMember_IdAndPlace_PlaceId(memberId, placeId)
                .orElse(null);

        if (favorite != null) {
            memberFavoriteRepository.delete(favorite);
        }
    }

    @Transactional(readOnly = true)
    public List<FavoritePlaceResponseDto> getMyFavorites(Long memberId, String lang) {
        List<MemberFavorite> favorites = memberFavoriteRepository.findAllByMember_Id(memberId);
        List<FavoritePlaceResponseDto> result = new ArrayList<>();

        for (MemberFavorite favorite : favorites) {
            TourPlace place = favorite.getPlace();

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

            result.add(new FavoritePlaceResponseDto(
                    place.getPlaceId(),
                    title,
                    content,
                    imageUrl,
                    categoryName,
                    regionName
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
    public List<Long> getMyFavoritePlaceIds(Long memberId) {
        List<MemberFavorite> favorites = memberFavoriteRepository.findAllByMember_Id(memberId);
        List<Long> result = new ArrayList<>();

        for (MemberFavorite favorite : favorites) {
            result.add(favorite.getPlace().getPlaceId());
        }

        return result;
    }
}