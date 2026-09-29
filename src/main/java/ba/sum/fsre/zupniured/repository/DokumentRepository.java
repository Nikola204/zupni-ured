package ba.sum.fsre.zupniured.repository;

import ba.sum.fsre.zupniured.model.Dokument;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.domain.Sort;
import java.util.*;

public interface DokumentRepository extends JpaRepository<Dokument, Long>, JpaSpecificationExecutor<Dokument> {
    @Override
    @EntityGraph(attributePaths = {"zupljanin", "izdaoKorisnik", "krstenje", "prvaPricest", "krizma", "vjencanje"})
    Optional<Dokument> findById(Long id);

    List<Dokument> findByZupljaninId(Long zupljaninId, Sort sort);
}
