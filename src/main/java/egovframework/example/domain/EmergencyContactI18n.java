package egovframework.example.domain;

import javax.persistence.*;

@Entity
@Table(
    name = "EMERGENCY_CONTACT_I18N",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_emergency_contact_locale",
        columnNames = {"contact_id", "locale"}
    ),
    indexes = {
        @Index(name = "idx_emergency_contact_i18n_contact", columnList = "contact_id"),
        @Index(name = "idx_emergency_contact_i18n_locale", columnList = "locale")
    }
)
public class EmergencyContactI18n {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "i18n_id")
    private Long i18nId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "contact_id")
    private EmergencyContact contact;

    @Column(name = "locale", nullable = false, length = 20)
    private String locale;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "description", nullable = false, length = 500)
    private String description;

    @Column(name = "badge_label", nullable = false, length = 100)
    private String badgeLabel;

    public Long getI18nId() { return i18nId; }
    public EmergencyContact getContact() { return contact; }
    public void setContact(EmergencyContact contact) { this.contact = contact; }
    public String getLocale() { return locale; }
    public void setLocale(String locale) { this.locale = locale; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getBadgeLabel() { return badgeLabel; }
    public void setBadgeLabel(String badgeLabel) { this.badgeLabel = badgeLabel; }
}
