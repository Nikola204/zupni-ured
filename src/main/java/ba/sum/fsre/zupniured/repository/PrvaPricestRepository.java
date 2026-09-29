package ba.sum.fsre.zupniured.repository;

import ba.sum.fsre.zupniured.model.PrvaPricest;
import ba.sum.fsre.zupniured.model.Zupljanin;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PrvaPricestRepository extends JpaRepository<PrvaPricest, Long> {

    Optional<PrvaPricest> findByZupljaninId(Long zupljaninId);

    boolean existsByZupljaninId(Long zupljaninId);

    boolean existsByZupljaninIdAndIdNot(Long zupljaninId, Long id);

    @Query("""
            select z from Zupljanin z
            where not exists (
                select p.id from PrvaPricest p
                where p.zupljanin = z and (:pricestId is null or p.id <> :pricestId)
            )
            """)
    List<Zupljanin> pronadjiZupljaneZaIzbor(@Param("pricestId") Long pricestId, Sort sort);

    @Override
    @EntityGraph(attributePaths = "zupljanin")
    List<PrvaPricest> findAll(Sort sort);

    @Override
    @EntityGraph(attributePaths = "zupljanin")
    Optional<PrvaPricest> findById(Long id);

    @EntityGraph(attributePaths = "zupljanin")
    @Query("""
            select k from PrvaPricest k
            where locate(lower(:pojam), lower(k.zupljanin.ime)) > 0
               or locate(lower(:pojam), lower(k.zupljanin.prezime)) > 0
               or locate(lower(:pojam), lower(k.brojMatice)) > 0
               or k.godinaMatice = :godina
            """)
    List<PrvaPricest> pretrazi(@Param("pojam") String pojam, @Param("godina") Integer godina, Sort sort);
}
