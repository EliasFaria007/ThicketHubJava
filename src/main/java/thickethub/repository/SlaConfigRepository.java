package thickethub.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import thickethub.domain.enums.PrioridadeChamado;
import thickethub.domain.model.SlaConfig;

import java.util.Optional;

public interface SlaConfigRepository extends JpaRepository<SlaConfig, Long>, JpaSpecificationExecutor<SlaConfig> {
    Optional<SlaConfig> findByPrioridade(PrioridadeChamado prioridade);
    boolean existsByPrioridade(PrioridadeChamado prioridade);
}
