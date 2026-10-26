package thickethub.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import thickethub.domain.enums.Papel;
import thickethub.domain.model.Usuario;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long>, JpaSpecificationExecutor<Usuario> {
    Optional<Usuario> findByEmail(String email);
    Optional<Usuario> findByTelefone(String telefone);
    boolean existsByEmail(String email);
    List<Usuario> findAllByPapel(Papel papel);
    List<Usuario> findBySetor(String setor);
    long countByAtivoTrue();
    long countByPapel(Papel papel);

    @Query("SELECT u.papel, COUNT(u) FROM Usuario u GROUP BY u.papel")
    List<Object[]> countUsuariosAgrupadosPorPapel();
}
