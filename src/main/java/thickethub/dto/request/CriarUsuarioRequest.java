package thickethub.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import thickethub.domain.enums.Papel;

import java.util.Set;

public record CriarUsuarioRequest(
        @NotBlank String nome,
        @NotBlank @Email String email,
        @NotNull Papel papel,
        String setor,
        String telefone,
        String localidade,
        Boolean podeCadastrarUsuarios,
        Set<String> setores
) {}
