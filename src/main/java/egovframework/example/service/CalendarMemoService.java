package egovframework.example.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import egovframework.example.domain.CalendarMemo;
import egovframework.example.domain.Member;
import egovframework.example.dto.calendar.CalendarMemoRequestDto;
import egovframework.example.dto.calendar.CalendarMemoResponseDto;
import egovframework.example.repository.CalendarMemoRepository;
import egovframework.example.repository.MemberRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class CalendarMemoService {

    private final CalendarMemoRepository calendarMemoRepository;
    private final MemberRepository memberRepository;

    public CalendarMemoResponseDto addMemo(Long memberId, CalendarMemoRequestDto requestDto) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        // 항상 새로운 메모 생성 (같은 날짜에 여러 개 가능)
        CalendarMemo memo = new CalendarMemo();
        memo.setMember(member);
        memo.setStartDate(requestDto.getStartDate());
        memo.setMemo(requestDto.getMemo());
        calendarMemoRepository.save(memo);

        return new CalendarMemoResponseDto(
                memo.getMemoId(),
                memo.getStartDate(),
                memo.getMemo()
        );
    }

    public void deleteMemo(Long memberId, Long memoId) {
        CalendarMemo memo = calendarMemoRepository.findById(memoId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 메모입니다."));

        if (!memo.getMember().getId().equals(memberId)) {
            throw new IllegalArgumentException("권한이 없습니다.");
        }

        calendarMemoRepository.delete(memo);
    }

    @Transactional(readOnly = true)
    public List<CalendarMemoResponseDto> getMyMemos(Long memberId) {
        List<CalendarMemo> memos = calendarMemoRepository.findAllByMember_Id(memberId);
        List<CalendarMemoResponseDto> result = new ArrayList<>();

        for (CalendarMemo memo : memos) {
            result.add(new CalendarMemoResponseDto(
                    memo.getMemoId(),
                    memo.getStartDate(),
                    memo.getMemo()
            ));
        }

        return result;
    }

    @Transactional(readOnly = true)
    public CalendarMemoResponseDto getMemoByDate(Long memberId, LocalDate startDate) {
        CalendarMemo memo = calendarMemoRepository
                .findByMember_IdAndStartDate(memberId, startDate)
                .orElse(null);

        if (memo == null) {
            return null;
        }

        return new CalendarMemoResponseDto(
                memo.getMemoId(),
                memo.getStartDate(),
                memo.getMemo()
        );
    }

    @Transactional(readOnly = true)
    public CalendarMemoResponseDto getMemoById(Long memberId, Long memoId) {
        CalendarMemo memo = calendarMemoRepository.findById(memoId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 메모입니다."));

        if (!memo.getMember().getId().equals(memberId)) {
            throw new IllegalArgumentException("권한이 없습니다.");
        }

        return new CalendarMemoResponseDto(
                memo.getMemoId(),
                memo.getStartDate(),
                memo.getMemo()
        );
    }

    public CalendarMemoResponseDto updateMemo(Long memberId, Long memoId, CalendarMemoRequestDto requestDto) {
        CalendarMemo memo = calendarMemoRepository.findById(memoId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 메모입니다."));

        if (!memo.getMember().getId().equals(memberId)) {
            throw new IllegalArgumentException("권한이 없습니다.");
        }

        memo.setStartDate(requestDto.getStartDate());
        memo.setMemo(requestDto.getMemo());
        calendarMemoRepository.save(memo);

        return new CalendarMemoResponseDto(
                memo.getMemoId(),
                memo.getStartDate(),
                memo.getMemo()
        );
    }
}
