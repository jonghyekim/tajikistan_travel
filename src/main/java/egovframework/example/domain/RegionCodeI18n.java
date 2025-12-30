package egovframework.example.domain;

import javax.persistence.*;

@Entity
@Table(
    name = "REGION_CODE_I18N",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_region_code_locale",
        columnNames = {"code", "locale"}
    ),
    indexes = {
        @Index(name = "idx_region_i18n_code", columnList = "code"),
        @Index(name = "idx_region_i18n_locale", columnList = "locale")
    }
)
public class RegionCodeI18n {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "i18n_id")
    private Long i18nId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "code", referencedColumnName = "code")
    private RegionCode regionCode;

    @Column(name = "locale", nullable = false, length = 20)
    private String locale;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    // getters/setters ...
    public Long getI18nId() { return i18nId; }
    public RegionCode getRegionCode() { return regionCode; }
    public void setRegionCode(RegionCode regionCode) { this.regionCode = regionCode; }
    public String getLocale() { return locale; }
    public void setLocale(String locale) { this.locale = locale; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
