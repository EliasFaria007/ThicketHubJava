package thickethub.repository.specification;

import org.springframework.data.jpa.domain.Specification;
import thickethub.domain.enums.Papel;
import thickethub.domain.model.Auditoria;
import thickethub.domain.model.Usuario;

import java.time.LocalDateTime;

public class AuditoriaSpecification {

    private AuditoriaSpecification() {}

    public static Specification<Auditoria> porVisibilidade(Usuario user) {
        return (root, query, cb) -> {
            if (user == null) return cb.disjunction();
            if (user.getPapel() == Papel.TECNICO) {
                return cb.equal(root.get("usuarioId"), user.getId());
            }
            if (user.getPapel() == Papel.ADMIN || user.getPapel() == Papel.SUPERUSUARIO || user.getPapel() == Papel.SUPER) {
                return cb.conjunction();
            }
            return cb.equal(root.get("usuarioId"), user.getId());
        };
    }

    public static Specification<Auditoria> porUsuario(Long usuarioId) {
        return (root, query, cb) -> usuarioId == null
                ? cb.conjunction()
                : cb.equal(root.get("usuarioId"), usuarioId);
    }

    public static Specification<Auditoria> porAcao(String acao) {
        return (root, query, cb) -> acao == null || acao.isBlank()
                ? cb.conjunction()
                : cb.like(cb.lower(root.get("acao")), "%" + acao.toLowerCase() + "%");
    }

    public static Specification<Auditoria> porPeriodo(LocalDateTime inicio, LocalDateTime fim) {
        return (root, query, cb) -> {
            if (inicio == null && fim == null) return cb.conjunction();
            if (inicio != null && fim != null) return cb.between(root.get("criadoEm"), inicio, fim);
            if (inicio != null) return cb.greaterThanOrEqualTo(root.get("criadoEm"), inicio);
            return cb.lessThanOrEqualTo(root.get("criadoEm"), fim);
        };
    }
}
