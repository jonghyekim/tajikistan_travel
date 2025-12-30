package egovframework.example.domain;

import javax.persistence.*;

@Entity
@Table(
    name = "CATEGORY_CODE_I18N",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_category_code_locale",
        columnNames = {"code", "locale"}
    ),
    indexes = {
        @Index(name = "idx_category_i18n_code", columnList = "code"),
        @Index(name = "idx_category_i18n_locale", columnList = "locale")
    }
)
public class CategoryCodeI18n {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "i18n_id")
    private Long i18nId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "code", referencedColumnName = "code")
    private CategoryCode categoryCode;

    @Column(name = "locale", nullable = false, length = 20)
    private String locale;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    // getters/setters ...
    public Long getI18nId() { return i18nId; }
    public CategoryCode getCategoryCode() { return categoryCode; }
    public void setCategoryCode(CategoryCode categoryCode) { this.categoryCode = categoryCode; }
    public String getLocale() { return locale; }
    public void setLocale(String locale) { this.locale = locale; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}

