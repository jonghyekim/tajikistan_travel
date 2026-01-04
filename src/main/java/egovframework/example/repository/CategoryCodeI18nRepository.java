package egovframework.example.repository;

import egovframework.example.domain.CategoryCodeI18n;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.Collection;

public interface CategoryCodeI18nRepository extends JpaRepository<CategoryCodeI18n, Long> {

    List<CategoryCodeI18n> findAllByCategoryCode_Code(String code);

    Optional<CategoryCodeI18n> findByCategoryCode_CodeAndLocale(String code, String locale);

    boolean existsByCategoryCode_CodeAndLocale(String code, String locale);

    void deleteAllByCategoryCode_Code(String code);
    
    List<CategoryCodeI18n> findByCategoryCode_CodeInAndLocale(Collection<String> codes, String locale);
}

