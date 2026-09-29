package ba.sum.fsre.zupniured.repository;
import ba.sum.fsre.zupniured.model.MisnaNakana;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
public interface MisnaNakanaRepository extends JpaRepository<MisnaNakana, Long>,
        JpaSpecificationExecutor<MisnaNakana> {
}
