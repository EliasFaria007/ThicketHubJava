package thickethub.security;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import thickethub.domain.enums.Papel;
import thickethub.domain.model.Usuario;
import thickethub.repository.ChamadoRepository;
import thickethub.repository.UsuarioRepository;

import java.util.Set;

@Component("chamadoSecurity")
@RequiredArgsConstructor
public class ChamadoSecurity {

    private final UsuarioRepository usuarioRepository;
    private final ChamadoRepository chamadoRepository;

    private boolean isAdministrador(Usuario user) {
        return user != null && (user.getPapel() == Papel.ADMIN || user.getPapel() == Papel.SUPERUSUARIO || user.getPapel() == Papel.SUPER);
    }

    @Transactional(readOnly = true)
    public boolean podeAtuarNoSetor(Long chamadoId, String email) {
        if (chamadoId == null || email == null) return false;

        Usuario user = usuarioRepository.findByEmail(email).orElse(null);
        if (user == null) return false;

        if (isAdministrador(user)) return true;
        if (user.getPapel() != Papel.TECNICO) return false;

        return chamadoRepository.findById(chamadoId)
                .map(chamado -> user.getSetoresLiberados() != null
                        && chamado.getSetor() != null
                        && user.getSetoresLiberados().contains(chamado.getSetor()))
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public boolean ehSolicitante(Long chamadoId, String email) {
        if (chamadoId == null || email == null) return false;

        return chamadoRepository.findById(chamadoId)
                .map(chamado -> chamado.getSolicitante() != null
                        && chamado.getSolicitante().getEmail() != null
                        && chamado.getSolicitante().getEmail().equalsIgnoreCase(email))
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public boolean ehTecnicoAtribuido(Long chamadoId, String email) {
        if (chamadoId == null || email == null) return false;

        return chamadoRepository.findById(chamadoId)
                .map(chamado -> chamado.getTecnico() != null
                        && chamado.getTecnico().getEmail() != null
                        && chamado.getTecnico().getEmail().equalsIgnoreCase(email))
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public boolean podeVisualizar(Long chamadoId, String email) {
        if (chamadoId == null || email == null) return false;

        Usuario user = usuarioRepository.findByEmail(email).orElse(null);
        if (user == null) return false;

        if (isAdministrador(user)) return true;

        if (user.getPapel() == Papel.TECNICO) {
            return podeAtuarNoSetor(chamadoId, email) || ehTecnicoAtribuido(chamadoId, email);
        }

        return ehSolicitante(chamadoId, email);
    }

    @Transactional(readOnly = true)
    public boolean podeEncaminharPara(Long chamadoId, String email, String setorDestino) {
        if (chamadoId == null || email == null) return false;

        Usuario user = usuarioRepository.findByEmail(email).orElse(null);
        if (user == null) return false;

        if (isAdministrador(user)) return true;
        if (user.getPapel() != Papel.TECNICO) return false;

        Set<String> liberados = user.getSetoresLiberados();
        if (liberados == null || liberados.isEmpty()) return false;

        return chamadoRepository.findById(chamadoId)
                .map(chamado -> {
                    boolean podeNoOrigem = chamado.getSetor() != null && liberados.contains(chamado.getSetor());
                    if (setorDestino == null || setorDestino.isBlank()) {
                        return podeNoOrigem;
                    }
                    return podeNoOrigem;
                })
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public boolean podeVisualizarPorProtocolo(String protocolo, String email) {
        if (protocolo == null || email == null) return false;
        return chamadoRepository.findByProtocolo(protocolo)
                .map(chamado -> podeVisualizar(chamado.getId(), email))
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public boolean ehMesmoUsuario(Long usuarioId, String email) {
        if (usuarioId == null || email == null) return false;
        return usuarioRepository.findById(usuarioId)
                .map(u -> email.equalsIgnoreCase(u.getEmail()))
                .orElse(false);
    }
}