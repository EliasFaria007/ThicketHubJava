package thickethub.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import thickethub.domain.model.Notificacao;

import java.util.List;

public interface NotificacaoRepository extends JpaRepository<Notificacao, Long>, JpaSpecificationExecutor<Notificacao> {
    Page<Notificacao> findByUsuario_IdOrderByCriadoEmDesc(Long usuarioId, Pageable pageable);
    List<Notificacao> findTop20ByUsuario_IdOrderByCriadoEmDesc(Long usuarioId);
    List<Notificacao> findByUsuario_IdAndLidaFalseOrderByCriadoEmDesc(Long usuarioId);
    List<Notificacao> findByUsuario_IdAndChamado_IdAndLidaFalse(Long usuarioId, Long chamadoId);
    long countByUsuario_IdAndLidaFalse(Long usuarioId);
    void deleteByUsuario_Id(Long usuarioId);
}
