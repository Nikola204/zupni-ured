package ba.sum.fsre.zupniured.repository;

import ba.sum.fsre.zupniured.model.Vjencanje;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface VjencanjeRepository extends JpaRepository<Vjencanje, Long> {
    List<Vjencanje> findByMladozenjaZupljaninIdOrNevjestaZupljaninId(
            Long mladozenjaId, Long nevjestaId, Sort sort);

    @Override
    @EntityGraph(attributePaths = {"mladozenjaZupljanin", "nevjestaZupljanin"})
    Optional<Vjencanje> findById(Long id);

    @Query("""
            select v from Vjencanje v
            where locate(lower(:pojam), lower(v.imeMladozenje)) > 0
               or locate(lower(:pojam), lower(v.prezimeMladozenje)) > 0
               or locate(lower(:pojam), lower(v.imeNevjeste)) > 0
               or locate(lower(:pojam), lower(v.prezimeNevjeste)) > 0
               or locate(lower(:pojam), lower(v.brojMatice)) > 0
               or v.godinaMatice = :godina
            """)
    List<Vjencanje> pretrazi(@Param("pojam") String pojam, @Param("godina") Integer godina, Sort sort);
}
