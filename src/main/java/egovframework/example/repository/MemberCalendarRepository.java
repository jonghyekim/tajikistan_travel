package egovframework.example.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import egovframework.example.domain.MemberCalendar;

public interface MemberCalendarRepository extends JpaRepository<MemberCalendar, Long> {
    boolean existsByMember_IdAndPlace_PlaceIdAndStartDate(Long memberId, Long placeId, LocalDate startDate);
    Optional<MemberCalendar> findByMember_IdAndPlace_PlaceIdAndStartDate(Long memberId, Long placeId, LocalDate startDate);
    List<MemberCalendar> findAllByMember_Id(Long memberId);
    List<MemberCalendar> findAllByMember_IdAndStartDate(Long memberId, LocalDate startDate);
    void deleteByIdAndMember_Id(Long id, Long memberId);
}