package thickethub.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import thickethub.domain.model.Comentario;

import java.util.List;

public interface ComentarioRepository extends JpaRepository<Comentario, Long>, JpaSpecificationExecutor<Comentario> {
    List<Comentario> findByChamado_IdOrderByDataCriacaoAsc(Long chamadoId);
    List<Comentario> findByChamado_IdAndFlagInternoFalseOrderByDataCriacaoAsc(Long chamadoId);
}
