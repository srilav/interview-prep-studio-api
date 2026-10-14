package dev.prepstudio.catalog.domain;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;

public interface KeyRepository extends JpaRepository<Key, String> {

    List<Key> findBySubcategoryIdOrderByTermAsc(String subcategoryId);

    List<Key> findBySubcategoryIdAndStatusOrderByTermAsc(String subcategoryId, KeyStatus status);

    @Query("select k from Key k where k.subcategoryId = :subcategoryId and lower(k.term) = lower(:term)")
    List<Key> findBySubcategoryAndTerm(String subcategoryId, String term);

    @Query("select k.subcategoryId, count(k) from Key k group by k.subcategoryId")
    List<Object[]> countPerSubcategory();

    @Query("select k from Key k where k.status = :status and k.searchText like :pattern order by k.term")
    List<Key> search(KeyStatus status, String pattern, Pageable page);

    @Query("select k from Key k where k.status = :status and k.searchText like :pattern "
            + "and k.subcategoryId in :subcategoryIds order by k.term")
    List<Key> searchWithin(KeyStatus status, String pattern, Collection<String> subcategoryIds, Pageable page);
}
