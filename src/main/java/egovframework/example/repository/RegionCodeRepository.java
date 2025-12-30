package egovframework.example.repository;

import egovframework.example.domain.RegionCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RegionCodeRepository extends JpaRepository<RegionCode, String> {

    List<RegionCode> findAllByIsActiveTrueOrderBySortOrderAsc();
    List<RegionCode> findAllByIsActiveFalseOrderBySortOrderAsc();

    boolean existsByCode(String code);
}
