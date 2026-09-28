package thickethub.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import thickethub.domain.model.HistoricoChamado;

import java.util.List;

public interface HistoricoRepository extends JpaRepository<HistoricoChamado, Long>, JpaSpecificationExecutor<HistoricoChamado> {
    List<HistoricoChamado> findByChamadoIdOrderByDataEventoAsc(Long chamadoId);
}
