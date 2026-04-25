package egovframework.example.domain;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
    name = "EMERGENCY_CONTACT",
    indexes = {
        @Index(name = "idx_emergency_contact_active_order", columnList = "is_active, sort_order")
    }
)
public class EmergencyContact {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "contact_id")
    private Long contactId;

    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code;

    @Column(name = "phone_dial", nullable = false, length = 50)
    private String phoneDial;

    @Column(name = "phone_display", nullable = false, length = 50)
    private String phoneDisplay;

    @Column(name = "badge_type", nullable = false, length = 30)
    private String badgeType;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @OneToMany(mappedBy = "contact", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EmergencyContactI18n> i18ns = new ArrayList<>();

    @Transient
    private EmergencyContactI18n displayI18n;

    public void addI18n(EmergencyContactI18n i18n) {
        i18ns.add(i18n);
        i18n.setContact(this);
    }

    public Long getContactId() { return contactId; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getPhoneDial() { return phoneDial; }
    public void setPhoneDial(String phoneDial) { this.phoneDial = phoneDial; }
    public String getPhoneDisplay() { return phoneDisplay; }
    public void setPhoneDisplay(String phoneDisplay) { this.phoneDisplay = phoneDisplay; }
    public String getBadgeType() { return badgeType; }
    public void setBadgeType(String badgeType) { this.badgeType = badgeType; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean active) { isActive = active; }
    public List<EmergencyContactI18n> getI18ns() { return i18ns; }
    public EmergencyContactI18n getDisplayI18n() { return displayI18n; }
    public void setDisplayI18n(EmergencyContactI18n displayI18n) { this.displayI18n = displayI18n; }
}
