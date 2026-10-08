package thickethub.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import thickethub.domain.model.ArtigoBaseConhecimento;

import java.util.List;
import java.util.Optional;

public interface ArtigoBaseConhecimentoRepository
        extends JpaRepository<ArtigoBaseConhecimento, Long>, JpaSpecificationExecutor<ArtigoBaseConhecimento> {

    Optional<ArtigoBaseConhecimento> findBySlug(String slug);

    List<ArtigoBaseConhecimento> findBySetorIgnoreCase(String setor);
}
