package egovframework.example.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import egovframework.example.domain.MemberFavorite;

public interface MemberFavoriteRepository extends JpaRepository<MemberFavorite, Long> {
    boolean existsByMember_IdAndPlace_PlaceId(Long memberId, Long placeId);
    Optional<MemberFavorite> findByMember_IdAndPlace_PlaceId(Long memberId, Long placeId);
    List<MemberFavorite> findAllByMember_Id(Long memberId);
}
