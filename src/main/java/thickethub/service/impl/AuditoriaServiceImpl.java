package thickethub.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import thickethub.domain.model.Auditoria;
import thickethub.domain.model.Usuario;
import thickethub.dto.response.AuditoriaResponse;
import thickethub.dto.response.PaginacaoResponse;
import thickethub.repository.AuditoriaRepository;
import thickethub.repository.specification.AuditoriaSpecification;
import thickethub.service.AuditoriaService;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditoriaServiceImpl implements AuditoriaService {

    private final AuditoriaRepository auditoriaRepository;

    @Override
    @Transactional
    public void registrar(Usuario usuario, String acao, String detalhe, String ipOrigem) {
        try {
            Auditoria a = Auditoria.builder()
                    .acao(acao)
                    .detalhe(detalhe)
                    .ipOrigem(ipOrigem != null ? ipOrigem : "0.0.0.0")
                    .criadoEm(LocalDateTime.now())
                    .build();

            if (usuario != null) {
                a.setUsuarioId(usuario.getId());
                a.setUsuarioNome(usuario.getNome());
            }

            auditoriaRepository.save(a);
        } catch (Exception e) {
            log.warn("Erro ao salvar log de auditoria: {}", e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PaginacaoResponse<AuditoriaResponse> listar(
            Usuario usuarioLogado,
            Long usuarioId,
            String acao,
            LocalDateTime inicio,
            LocalDateTime fim,
            Pageable pageable) {

        Specification<Auditoria> spec = Specification
                .where(AuditoriaSpecification.porVisibilidade(usuarioLogado))
                .and(AuditoriaSpecification.porUsuario(usuarioId))
                .and(AuditoriaSpecification.porAcao(acao))
                .and(AuditoriaSpecification.porPeriodo(inicio, fim));

        Page<Auditoria> page = auditoriaRepository.findAll(spec, pageable);
        return PaginacaoResponse.de(page.map(AuditoriaResponse::de));
    }
}
