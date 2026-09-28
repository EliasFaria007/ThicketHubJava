package thickethub.dto.response;

import thickethub.domain.enums.Papel;
import thickethub.domain.model.Usuario;

import java.time.LocalDateTime;
import java.util.Set;

public record UsuarioResponse(
        Long id,
        String nome,
        String email,
        Papel papel,
        String setor,
        String telefone,
        String localidade,
        Boolean podeCadastrarUsuarios,
        Boolean primeiroAcessoConcluido,
        Boolean ativo,
        LocalDateTime criadoEm,
        Set<String> setoresLiberados,
        String senhaTemporaria
) {
    public static UsuarioResponse de(Usuario usuario) {
        return de(usuario, null);
    }

    public static UsuarioResponse de(Usuario usuario, String senhaTemporaria) {
        if (usuario == null) return null;
        return new UsuarioResponse(
                usuario.getId(),
                usuario.getNome(),
                usuario.getEmail(),
                usuario.getPapel(),
                usuario.getSetor(),
                usuario.getTelefone(),
                usuario.getLocalidade(),
                usuario.getPodeCadastrarUsuarios(),
                usuario.getPrimeiroAcessoConcluido(),
                usuario.getAtivo(),
                usuario.getCriadoEm(),
                usuario.getSetoresLiberados(),
                senhaTemporaria
        );
    }
}
