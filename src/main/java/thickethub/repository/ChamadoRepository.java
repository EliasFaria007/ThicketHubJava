package thickethub.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import thickethub.domain.enums.StatusChamado;
import thickethub.domain.model.Chamado;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ChamadoRepository extends JpaRepository<Chamado, Long>, JpaSpecificationExecutor<Chamado> {
    Optional<Chamado> findByProtocolo(String protocolo);
    boolean existsByProtocolo(String protocolo);
    List<Chamado> findBySolicitanteId(Long solicitanteId);
    List<Chamado> findByTecnicoId(Long tecnicoId);
    long countByStatus(StatusChamado status);
    long countBySetor(String setor);
    List<Chamado> findByStatusIn(List<StatusChamado> statuses);

    @Query("SELECT c FROM Chamado c WHERE c.status IN :statuses AND c.slaRodandoDesde IS NOT NULL")
    List<Chamado> findChamadosComSlaAtivo(@Param("statuses") List<StatusChamado> statuses);

    @Query("SELECT COUNT(c) FROM Chamado c WHERE c.dataAbertura >= :inicio AND c.dataAbertura <= :fim")
    long countAbertosNoPeriodo(@Param("inicio") LocalDateTime inicio, @Param("fim") LocalDateTime fim);
}
