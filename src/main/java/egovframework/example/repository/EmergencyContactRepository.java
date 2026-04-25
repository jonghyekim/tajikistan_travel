package egovframework.example.repository;

import egovframework.example.domain.EmergencyContact;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EmergencyContactRepository extends JpaRepository<EmergencyContact, Long> {

    @EntityGraph(attributePaths = {"i18ns"})
    List<EmergencyContact> findAllByIsActiveTrueOrderBySortOrderAsc();

    boolean existsByCode(String code);
}
