package egovframework.example.domain;

import java.time.LocalDate;
import java.time.Instant;
import javax.persistence.*;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import lombok.*;

@Entity
@Table(
    name = "calendar_memo",
    indexes = {
        @Index(name = "idx_member_date", columnList = "member_id, start_date")
    }
)
@Getter
@Setter
@NoArgsConstructor
public class CalendarMemo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long memoId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "memo", columnDefinition = "TEXT")
    private String memo;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
