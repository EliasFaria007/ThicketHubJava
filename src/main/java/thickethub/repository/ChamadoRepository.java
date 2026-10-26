package thickethub.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
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

    @Override
    @EntityGraph(attributePaths = {"solicitante", "tecnico", "servico"})
    List<Chamado> findAll();

    @Override
    @EntityGraph(attributePaths = {"solicitante", "tecnico", "servico"})
    Page<Chamado> findAll(Specification<Chamado> spec, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"solicitante", "tecnico", "servico"})
    Optional<Chamado> findById(Long id);

    @EntityGraph(attributePaths = {"solicitante", "tecnico", "servico"})
    Optional<Chamado> findByProtocolo(String protocolo);

    boolean existsByProtocolo(String protocolo);

    @EntityGraph(attributePaths = {"solicitante", "tecnico", "servico"})
    List<Chamado> findBySolicitante_Id(Long solicitanteId);

    @EntityGraph(attributePaths = {"solicitante", "tecnico", "servico"})
    List<Chamado> findByTecnico_Id(Long tecnicoId);

    long countByStatus(StatusChamado status);
    long countBySetor(String setor);

    @EntityGraph(attributePaths = {"solicitante", "tecnico", "servico"})
    List<Chamado> findByStatusIn(List<StatusChamado> statuses);

    @EntityGraph(attributePaths = {"solicitante", "tecnico", "servico"})
    @Query("SELECT c FROM Chamado c WHERE c.status IN :statuses AND c.slaRodandoDesde IS NOT NULL")
    List<Chamado> findChamadosComSlaAtivo(@Param("statuses") List<StatusChamado> statuses);

    @Query("SELECT COUNT(c) FROM Chamado c WHERE c.dataAbertura >= :inicio AND c.dataAbertura <= :fim")
    long countAbertosNoPeriodo(@Param("inicio") LocalDateTime inicio, @Param("fim") LocalDateTime fim);

    @Query("SELECT c.status, COUNT(c) FROM Chamado c GROUP BY c.status")
    List<Object[]> countChamadosAgrupadosPorStatus();
}
