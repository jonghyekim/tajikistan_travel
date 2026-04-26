package egovframework.example.domain;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
    name = "TOUR_PLACE",
    indexes = {
        @Index(name = "idx_tour_place_category", columnList = "category_code"),
        @Index(name = "idx_tour_place_region", columnList = "region_code")
    }
)
public class TourPlace {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "place_id")
    private Long placeId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_code", referencedColumnName = "code")
    private CategoryCode category;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "region_code", referencedColumnName = "code")
    private RegionCode region;

    @Column(name = "address")
    private String address;

    @Column(name = "open_time")
    private String openTime;

    @Column(name = "close_time")
    private String closeTime;

    @Column(name = "is_free")
    private Boolean isFree;

    @Column(name = "currency_code")
    private String currencyCode;

    @Column(name = "admission_fee")
    private String admissionFee;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "place", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TourPlaceI18n> i18ns = new ArrayList<>();

    @OneToMany(mappedBy = "place", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Image> images = new ArrayList<>();

    @Transient
    private TourPlaceI18n displayI18n;

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public void addI18n(TourPlaceI18n i18n) {
        i18ns.add(i18n);
        i18n.setPlace(this);
    }

    public void addImage(Image image) {
        images.add(image);
        image.setPlace(this);
    }

    public Long getPlaceId() {
        return placeId;
    }

    public CategoryCode getCategory() {
        return category;
    }

    public void setCategory(CategoryCode category) {
        this.category = category;
    }

    public RegionCode getRegion() {
        return region;
    }

    public void setRegion(RegionCode region) {
        this.region = region;
    }

    public String getAddress() {
        return address;
    }

    public String getOpenTime() {
        return openTime;
    }

    public String getCloseTime() {
        return closeTime;
    }

    public Boolean getIsFree() {
        return isFree;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public String getAdmissionFee() {
        return admissionFee;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean active) {
        isActive = active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public List<TourPlaceI18n> getI18ns() {
        return i18ns;
    }

    public List<Image> getImages() {
        return images;
    }

    public TourPlaceI18n getDisplayI18n() {
        return displayI18n;
    }

    public void setDisplayI18n(TourPlaceI18n displayI18n) {
        this.displayI18n = displayI18n;
    }
}