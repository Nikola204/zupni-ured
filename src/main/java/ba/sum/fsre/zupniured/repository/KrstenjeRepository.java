package ba.sum.fsre.zupniured.repository;

import ba.sum.fsre.zupniured.model.Krstenje;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface KrstenjeRepository extends JpaRepository<Krstenje, Long> {

    Optional<Krstenje> findByZupljaninId(Long zupljaninId);

    boolean existsByZupljaninId(Long zupljaninId);

    boolean existsByZupljaninIdAndIdNot(Long zupljaninId, Long id);

    @Override
    @EntityGraph(attributePaths = "zupljanin")
    List<Krstenje> findAll(Sort sort);

    @Override
    @EntityGraph(attributePaths = "zupljanin")
    Optional<Krstenje> findById(Long id);

    @EntityGraph(attributePaths = "zupljanin")
    @Query("""
            select k from Krstenje k
            where locate(lower(:pojam), lower(k.zupljanin.ime)) > 0
               or locate(lower(:pojam), lower(k.zupljanin.prezime)) > 0
               or locate(lower(:pojam), lower(k.brojMatice)) > 0
               or k.godinaMatice = :godina
            """)
    List<Krstenje> pretrazi(@Param("pojam") String pojam, @Param("godina") Integer godina, Sort sort);
}
