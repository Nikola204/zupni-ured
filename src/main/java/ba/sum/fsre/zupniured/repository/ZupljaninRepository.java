package ba.sum.fsre.zupniured.repository;

import ba.sum.fsre.zupniured.model.Zupljanin;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ZupljaninRepository extends JpaRepository<Zupljanin, Long> {
    long countByAktivanTrue();

    List<Zupljanin> findByImeContainingIgnoreCaseOrPrezimeContainingIgnoreCase(
            String ime, String prezime, Sort sort);
}
