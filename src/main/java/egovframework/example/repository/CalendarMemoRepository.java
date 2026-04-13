package egovframework.example.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import egovframework.example.domain.CalendarMemo;

@Repository
public interface CalendarMemoRepository extends JpaRepository<CalendarMemo, Long> {
    List<CalendarMemo> findAllByMember_Id(Long memberId);
    
    Optional<CalendarMemo> findByMember_IdAndStartDate(Long memberId, LocalDate startDate);
    
    List<CalendarMemo> findAllByMember_IdAndStartDate(Long memberId, LocalDate startDate);
    
    void deleteByMemoIdAndMember_Id(Long memoId, Long memberId);
}
