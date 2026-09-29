package ba.sum.fsre.zupniured.repository;

import ba.sum.fsre.zupniured.model.Krizma;
import ba.sum.fsre.zupniured.model.Zupljanin;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface KrizmaRepository extends JpaRepository<Krizma, Long> {

    Optional<Krizma> findByZupljaninId(Long zupljaninId);

    boolean existsByZupljaninId(Long zupljaninId);

    boolean existsByZupljaninIdAndIdNot(Long zupljaninId, Long id);

    @Query("""
            select z from Zupljanin z
            where not exists (
                select p.id from Krizma p
                where p.zupljanin = z and (:krizmaId is null or p.id <> :krizmaId)
            )
            """)
    List<Zupljanin> pronadjiZupljaneZaIzbor(@Param("krizmaId") Long krizmaId, Sort sort);

    @Override
    @EntityGraph(attributePaths = "zupljanin")
    List<Krizma> findAll(Sort sort);

    @Override
    @EntityGraph(attributePaths = "zupljanin")
    Optional<Krizma> findById(Long id);

    @EntityGraph(attributePaths = "zupljanin")
    @Query("""
            select k from Krizma k
            where locate(lower(:pojam), lower(k.zupljanin.ime)) > 0
               or locate(lower(:pojam), lower(k.zupljanin.prezime)) > 0
               or locate(lower(:pojam), lower(k.brojMatice)) > 0
               or k.godinaMatice = :godina
            """)
    List<Krizma> pretrazi(@Param("pojam") String pojam, @Param("godina") Integer godina, Sort sort);
}
