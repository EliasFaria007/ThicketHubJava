package thickethub.repository.specification;

import org.springframework.data.jpa.domain.Specification;
import thickethub.domain.enums.Papel;
import thickethub.domain.enums.PrioridadeChamado;
import thickethub.domain.enums.StatusChamado;
import thickethub.domain.model.Chamado;
import thickethub.domain.model.Usuario;

public class ChamadoSpecification {

    private ChamadoSpecification() {}

    public static Specification<Chamado> porVisibilidade(Usuario user) {
        return (root, query, cb) -> {
            if (user == null) return cb.disjunction();

            if (user.getPapel() == Papel.ADMIN || user.getPapel() == Papel.SUPERUSUARIO || user.getPapel() == Papel.SUPER) {
                return cb.conjunction();
            }

            if (user.getPapel() == Papel.TECNICO) {
                if (user.getSetoresLiberados() != null && !user.getSetoresLiberados().isEmpty()) {
                    return cb.or(
                            root.get("setor").in(user.getSetoresLiberados()),
                            cb.equal(root.get("tecnico").get("id"), user.getId())
                    );
                }
                return cb.equal(root.get("tecnico").get("id"), user.getId());
            }

            // Usuário padrão: apenas chamados que ele mesmo abriu
            return cb.equal(root.get("solicitante").get("id"), user.getId());
        };
    }

    public static Specification<Chamado> porStatus(StatusChamado status) {
        return (root, query, cb) -> status == null
                ? cb.conjunction()
                : cb.equal(root.get("status"), status);
    }

    public static Specification<Chamado> porPrioridade(PrioridadeChamado prioridade) {
        return (root, query, cb) -> prioridade == null
                ? cb.conjunction()
                : cb.equal(root.get("prioridade"), prioridade);
    }

    public static Specification<Chamado> porServico(Long servicoId) {
        return (root, query, cb) -> servicoId == null
                ? cb.conjunction()
                : cb.equal(root.get("servico").get("id"), servicoId);
    }

    public static Specification<Chamado> porSetor(String setor) {
        return (root, query, cb) -> setor == null || setor.isBlank()
                ? cb.conjunction()
                : cb.equal(root.get("setor"), setor);
    }

    public static Specification<Chamado> porFila(String fila, Usuario user) {
        return (root, query, cb) -> {
            if (fila == null || fila.isBlank() || user == null) {
                return cb.conjunction();
            }
            if ("minha".equalsIgnoreCase(fila)) {
                return cb.and(
                    cb.equal(root.get("tecnico").get("id"), user.getId()),
                    cb.not(root.get("status").in(StatusChamado.RESOLVIDO, StatusChamado.FECHADO, StatusChamado.CONCLUIDO, StatusChamado.CANCELADO))
                );
            }
            if ("resolvidos".equalsIgnoreCase(fila)) {
                return cb.and(
                    cb.equal(root.get("tecnico").get("id"), user.getId()),
                    root.get("status").in(StatusChamado.RESOLVIDO, StatusChamado.FECHADO, StatusChamado.CONCLUIDO)
                );
            }
            if ("geral".equalsIgnoreCase(fila)) {
                return cb.and(
                    cb.isNull(root.get("tecnico")),
                    cb.not(root.get("status").in(StatusChamado.RESOLVIDO, StatusChamado.FECHADO, StatusChamado.CONCLUIDO, StatusChamado.CANCELADO))
                );
            }
            return cb.conjunction();
        };
    }

    public static Specification<Chamado> buscaTextual(String q) {
        return (root, query, cb) -> {
            if (q == null || q.isBlank()) return cb.conjunction();
            String like = "%" + q.toLowerCase() + "%";
            return cb.or(
                cb.like(cb.lower(root.get("protocolo")), like),
                cb.like(cb.lower(root.get("titulo")), like),
                cb.like(cb.lower(root.get("descricao")), like),
                cb.like(cb.lower(root.get("solicitante").get("nome")), like)
            );
        };
    }
}
