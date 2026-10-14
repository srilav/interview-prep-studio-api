package dev.prepstudio.catalog.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface PlatformRepository extends JpaRepository<Platform, String> {

    List<Platform> findAllByOrderByNameAsc();

    boolean existsByNameIgnoreCase(String name);

    @Query("select p from Platform p join p.categories c join c.subcategories s where s.id = :subcategoryId")
    Optional<Platform> findBySubcategoryId(String subcategoryId);
}
