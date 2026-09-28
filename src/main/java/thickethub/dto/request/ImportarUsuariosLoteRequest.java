package thickethub.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import thickethub.domain.enums.Papel;

import java.util.List;

public record ImportarUsuariosLoteRequest(
        @NotEmpty @Valid List<UsuarioImportacaoItem> usuarios
) {

    public record UsuarioImportacaoItem(
            @NotBlank String nome,
            @NotBlank @Email String email,
            @NotBlank String setor,
            @NotNull Papel papel
    ) {}
}
