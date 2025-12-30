package egovframework.example.repository;

import egovframework.example.domain.CategoryCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoryCodeRepository extends JpaRepository<CategoryCode, String> {

    List<CategoryCode> findAllByIsActiveTrueOrderBySortOrderAsc();
    List<CategoryCode> findAllByIsActiveFalseOrderBySortOrderAsc();

    boolean existsByCode(String code);
}