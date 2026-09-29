package thickethub.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import thickethub.domain.model.ChamadoVinculo;

import java.util.List;

public interface ChamadoVinculoRepository extends JpaRepository<ChamadoVinculo, Long>, JpaSpecificationExecutor<ChamadoVinculo> {
    List<ChamadoVinculo> findByChamadoOrigem_IdOrChamadoDestino_Id(Long origemId, Long destinoId);
    boolean existsByChamadoOrigem_IdAndChamadoDestino_Id(Long origemId, Long destinoId);
}
