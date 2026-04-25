package egovframework.example.chatbot.domain;

import egovframework.example.domain.TourPlace;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Enumerated;
import javax.persistence.EnumType;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import java.time.DayOfWeek;
import java.time.LocalTime;

@Entity
@Table(
    name = "TOUR_PLACE_OPERATING_HOUR",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_place_day_season",
        columnNames = {"place_id", "day_of_week", "season_code"}
    ),
    indexes = {
        @Index(name = "idx_operating_hour_place", columnList = "place_id"),
        @Index(name = "idx_operating_hour_active", columnList = "is_active")
    }
)
public class TourPlaceOperatingHour {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "operating_hour_id")
    private Long operatingHourId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "place_id", nullable = false)
    private TourPlace place;

    @Column(name = "day_of_week", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private DayOfWeek dayOfWeek;

    @Column(name = "season_code", nullable = false, length = 50)
    private String seasonCode = "ALL";

    @Column(name = "opens_at")
    private LocalTime opensAt;

    @Column(name = "closes_at")
    private LocalTime closesAt;

    @Column(name = "is_closed", nullable = false)
    private Boolean isClosed = false;

    @Column(name = "last_admission_at")
    private LocalTime lastAdmissionAt;

    @Column(name = "note", length = 500)
    private String note;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    public Long getOperatingHourId() {
        return operatingHourId;
    }

    public TourPlace getPlace() {
        return place;
    }

    public void setPlace(TourPlace place) {
        this.place = place;
    }

    public DayOfWeek getDayOfWeek() {
        return dayOfWeek;
    }

    public void setDayOfWeek(DayOfWeek dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }

    public String getSeasonCode() {
        return seasonCode;
    }

    public void setSeasonCode(String seasonCode) {
        this.seasonCode = seasonCode;
    }

    public LocalTime getOpensAt() {
        return opensAt;
    }

    public void setOpensAt(LocalTime opensAt) {
        this.opensAt = opensAt;
    }

    public LocalTime getClosesAt() {
        return closesAt;
    }

    public void setClosesAt(LocalTime closesAt) {
        this.closesAt = closesAt;
    }

    public Boolean getIsClosed() {
        return isClosed;
    }

    public void setIsClosed(Boolean closed) {
        isClosed = closed;
    }

    public LocalTime getLastAdmissionAt() {
        return lastAdmissionAt;
    }

    public void setLastAdmissionAt(LocalTime lastAdmissionAt) {
        this.lastAdmissionAt = lastAdmissionAt;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean active) {
        isActive = active;
    }
}
