package thickethub.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import thickethub.domain.model.Comentario;

import java.util.List;

public interface ComentarioRepository extends JpaRepository<Comentario, Long>, JpaSpecificationExecutor<Comentario> {
    List<Comentario> findByChamadoIdOrderByDataCriacaoAsc(Long chamadoId);
    List<Comentario> findByChamadoIdAndFlagInternoFalseOrderByDataCriacaoAsc(Long chamadoId);
}
