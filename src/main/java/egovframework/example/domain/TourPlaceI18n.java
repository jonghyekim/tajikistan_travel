package egovframework.example.domain;

import javax.persistence.*;

@Entity
@Table(
    name = "TOUR_PLACE_I18N",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_place_locale",
        columnNames = {"place_id", "locale"}
    ),
    indexes = {
        @Index(name = "idx_place_i18n_place", columnList = "place_id"),
        @Index(name = "idx_place_i18n_locale", columnList = "locale")
    }
)
public class TourPlaceI18n {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "i18n_id")
    private Long i18nId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "place_id")
    private TourPlace place;

    @Column(name = "locale", nullable = false, length = 20)
    private String locale;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Lob
    @Column(name = "content")
    private String content;

    @Column(name = "address", length = 255)
    private String address;

    // getters/setters ...
    public Long getI18nId() { return i18nId; }
    public TourPlace getPlace() { return place; }
    public void setPlace(TourPlace place) { this.place = place; }
    public String getLocale() { return locale; }
    public void setLocale(String locale) { this.locale = locale; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
}

