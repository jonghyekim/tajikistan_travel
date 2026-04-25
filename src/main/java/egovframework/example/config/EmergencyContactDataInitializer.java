package egovframework.example.config;

import egovframework.example.domain.EmergencyContact;
import egovframework.example.domain.EmergencyContactI18n;
import egovframework.example.repository.EmergencyContactRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class EmergencyContactDataInitializer implements ApplicationRunner {

    private final EmergencyContactRepository emergencyContactRepository;

    public EmergencyContactDataInitializer(EmergencyContactRepository emergencyContactRepository) {
        this.emergencyContactRepository = emergencyContactRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seed("general", "112", "112", "emergency", 10,
            text("en", "General Emergency", "Universal emergency line (Police / Ambulance / Fire)", "Emergency"),
            text("ko", "일반 긴급", "통합 긴급 전화 (경찰 / 구급 / 소방)", "긴급"),
            text("ru", "Общий экстренный вызов", "Единый номер (Полиция / Скорая / Пожарная)", "Экстренно"),
            text("tg", "Занги умумии изтирорӣ", "Рақами ягона (Пулис / Ёрии таъҷилӣ / Хатари сӯхтор)", "Изтирорӣ")
        );
        seed("police", "102", "102", "police", 20,
            text("en", "Police", "Emergency police assistance", "Police"),
            text("ko", "경찰", "경찰 긴급 지원", "경찰"),
            text("ru", "Полиция", "Экстренная помощь полиции", "Полиция"),
            text("tg", "Пулис", "Кӯмаки фаврии пулис", "Пулис")
        );
        seed("ambulance", "103", "103", "medical", 30,
            text("en", "Ambulance", "Medical emergency / ambulance dispatch", "Medical"),
            text("ko", "구급차", "의료 긴급 / 구급차 출동", "의료"),
            text("ru", "Скорая помощь", "Медицинская помощь / вызов скорой", "Медицинская"),
            text("tg", "Ёрии таъҷилӣ", "Кӯмаки тиббӣ / даъвати ёрии таъҷилӣ", "Тиббӣ")
        );
        seed("fire", "101", "101", "fire", 40,
            text("en", "Fire Service", "Fire emergency / rescue", "Fire"),
            text("ko", "소방서", "화재 긴급 / 구조", "소방"),
            text("ru", "Пожарная служба", "Пожарная тревога / спасение", "Пожар"),
            text("tg", "Хадамоти оташнишонӣ", "Сӯхтор / наҷот", "Сӯхтор")
        );
        seed("prospekt_clinic", "+992487024400", "+992 48 702 4400", "medical", 50,
            text("en", "Prospekt Clinic (Dushanbe)", "Hospital / clinic in Dushanbe", "Medical"),
            text("ko", "프로스펙트 클리닉 (두샨베)", "두샨베 병원 / 클리닉", "의료"),
            text("ru", "Клиника Проспект (Душанбе)", "Больница / клиника в Душанбе", "Медицинская"),
            text("tg", "Клиникаи Проспект (Душанбе)", "Беморхона / клиника дар Душанбе", "Тиббӣ")
        );
        seed("us_embassy", "+992372292000", "+992 372 29 20 00", "embassy", 60,
            text("en", "U.S. Embassy (Dushanbe)", "Embassy contact (consular assistance)", "Embassy"),
            text("ko", "미국 대사관 (두샨베)", "대사관 연락처 (영사 지원)", "대사관"),
            text("ru", "Посольство США (Душанбе)", "Контакт посольства (консульская помощь)", "Посольство"),
            text("tg", "Сафорати ИМА (Душанбе)", "Тамоси сафорат (кӯмаки консулӣ)", "Сафорат")
        );
    }

    private void seed(String code, String phoneDial, String phoneDisplay, String badgeType, int sortOrder, ContactText... texts) {
        if (emergencyContactRepository.existsByCode(code)) {
            return;
        }

        EmergencyContact contact = new EmergencyContact();
        contact.setCode(code);
        contact.setPhoneDial(phoneDial);
        contact.setPhoneDisplay(phoneDisplay);
        contact.setBadgeType(badgeType);
        contact.setSortOrder(sortOrder);
        contact.setIsActive(true);

        for (ContactText text : texts) {
            EmergencyContactI18n i18n = new EmergencyContactI18n();
            i18n.setLocale(text.locale);
            i18n.setTitle(text.title);
            i18n.setDescription(text.description);
            i18n.setBadgeLabel(text.badgeLabel);
            contact.addI18n(i18n);
        }

        emergencyContactRepository.save(contact);
    }

    private ContactText text(String locale, String title, String description, String badgeLabel) {
        return new ContactText(locale, title, description, badgeLabel);
    }

    private static class ContactText {
        private final String locale;
        private final String title;
        private final String description;
        private final String badgeLabel;

        private ContactText(String locale, String title, String description, String badgeLabel) {
            this.locale = locale;
            this.title = title;
            this.description = description;
            this.badgeLabel = badgeLabel;
        }
    }
}
