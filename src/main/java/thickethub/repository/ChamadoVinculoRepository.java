package thickethub.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import thickethub.domain.model.ChamadoVinculo;

import java.util.List;

public interface ChamadoVinculoRepository extends JpaRepository<ChamadoVinculo, Long>, JpaSpecificationExecutor<ChamadoVinculo> {
    List<ChamadoVinculo> findByChamadoOrigemIdOrChamadoDestinoId(Long origemId, Long destinoId);
    boolean existsByChamadoOrigemIdAndChamadoDestinoId(Long origemId, Long destinoId);
}
