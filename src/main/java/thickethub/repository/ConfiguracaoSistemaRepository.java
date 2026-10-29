package thickethub.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import thickethub.domain.model.ConfiguracaoSistema;

/**
 * Repositório JPA para as configurações corporativas persistidas no banco de dados.
 */
@Repository
public interface ConfiguracaoSistemaRepository extends JpaRepository<ConfiguracaoSistema, String> {
}
