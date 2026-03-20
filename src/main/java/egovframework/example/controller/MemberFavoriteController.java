package egovframework.example.controller;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import egovframework.example.dto.favorite.FavoritePlaceResponseDto;
import egovframework.example.service.MemberFavoriteService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/me/favorite")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class MemberFavoriteController {

    private final MemberFavoriteService memberFavoriteService;

    @PostMapping("/add/{placeId}")
    public void addFavorite(@PathVariable Long placeId,
                            Authentication authentication) {
        Long memberId = (Long) authentication.getPrincipal();
        memberFavoriteService.addFavorite(memberId, placeId);
    }

    @DeleteMapping("/delete/{placeId}")
    public void removeFavorite(@PathVariable Long placeId,
                               Authentication authentication) {
        Long memberId = (Long) authentication.getPrincipal();
        memberFavoriteService.removeFavorite(memberId, placeId);
    }

    @GetMapping("/list")
    public List<FavoritePlaceResponseDto> getMyFavorites(
            @RequestParam(defaultValue = "en") String lang,
            Authentication authentication) {
        Long memberId = (Long) authentication.getPrincipal();
        return memberFavoriteService.getMyFavorites(memberId, lang);
    }
    
    @GetMapping("/place-ids")
    public List<Long> getMyFavoritePlaceIds(Authentication authentication) {
        Long memberId = (Long) authentication.getPrincipal();
        return memberFavoriteService.getMyFavoritePlaceIds(memberId);
    }
}