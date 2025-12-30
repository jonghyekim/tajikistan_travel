package egovframework.example.repository;

import egovframework.example.domain.RegionCodeI18n;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RegionCodeI18nRepository extends JpaRepository<RegionCodeI18n, Long> {

    List<RegionCodeI18n> findAllByRegionCode_Code(String code);

    Optional<RegionCodeI18n> findByRegionCode_CodeAndLocale(String code, String locale);

    boolean existsByRegionCode_CodeAndLocale(String code, String locale);

    void deleteAllByRegionCode_Code(String code);
}
