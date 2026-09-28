package thickethub.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import thickethub.domain.model.Servico;

import java.util.List;

public interface ServicoRepository extends JpaRepository<Servico, Long>, JpaSpecificationExecutor<Servico> {
    List<Servico> findByAtivoTrue();
    List<Servico> findBySetorAndAtivoTrue(String setor);
    boolean existsByNomeAndSetor(String nome, String setor);
}
