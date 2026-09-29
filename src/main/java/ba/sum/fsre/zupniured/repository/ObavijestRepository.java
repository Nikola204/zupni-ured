package ba.sum.fsre.zupniured.repository;

import ba.sum.fsre.zupniured.model.Obavijest;
import org.springframework.data.jpa.repository.*;
import java.util.Optional;

public interface ObavijestRepository extends JpaRepository<Obavijest, Long>,
        JpaSpecificationExecutor<Obavijest> {
    @Override
    @EntityGraph(attributePaths = "autor")
    Optional<Obavijest> findById(Long id);
}
