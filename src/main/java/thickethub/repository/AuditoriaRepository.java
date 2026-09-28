package thickethub.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import thickethub.domain.model.Auditoria;

import java.util.List;

public interface AuditoriaRepository extends JpaRepository<Auditoria, Long>, JpaSpecificationExecutor<Auditoria> {
    List<Auditoria> findTop100ByOrderByCriadoEmDesc();
    List<Auditoria> findByUsuarioIdOrderByCriadoEmDesc(Long usuarioId);
}
