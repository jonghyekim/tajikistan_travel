package egovframework.example.service;

import egovframework.example.domain.EmergencyContact;
import egovframework.example.domain.EmergencyContactI18n;
import egovframework.example.repository.EmergencyContactRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class EmergencyContactService {

    private final EmergencyContactRepository emergencyContactRepository;

    public EmergencyContactService(EmergencyContactRepository emergencyContactRepository) {
        this.emergencyContactRepository = emergencyContactRepository;
    }

    @Transactional(readOnly = true)
    public List<EmergencyContact> findActiveContacts(String locale) {
        List<EmergencyContact> contacts = emergencyContactRepository.findAllByIsActiveTrueOrderBySortOrderAsc();
        contacts.forEach(contact -> contact.setDisplayI18n(resolveI18n(contact, locale)));
        return contacts;
    }

    private EmergencyContactI18n resolveI18n(EmergencyContact contact, String locale) {
        EmergencyContactI18n english = null;
        EmergencyContactI18n first = null;

        for (EmergencyContactI18n i18n : contact.getI18ns()) {
            if (first == null) {
                first = i18n;
            }
            if ("en".equals(i18n.getLocale())) {
                english = i18n;
            }
            if (locale != null && locale.equals(i18n.getLocale())) {
                return i18n;
            }
        }

        return english != null ? english : first;
    }
}
