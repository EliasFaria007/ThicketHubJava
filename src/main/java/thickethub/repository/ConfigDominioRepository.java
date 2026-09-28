package thickethub.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import thickethub.domain.model.ConfigDominio;

import java.util.List;
import java.util.Optional;

public interface ConfigDominioRepository extends JpaRepository<ConfigDominio, Long>, JpaSpecificationExecutor<ConfigDominio> {
    Optional<ConfigDominio> findByDominio(String dominio);
    boolean existsByDominio(String dominio);
    boolean existsByDominioAndAtivoTrue(String dominio);
    List<ConfigDominio> findByAtivoTrue();
}
