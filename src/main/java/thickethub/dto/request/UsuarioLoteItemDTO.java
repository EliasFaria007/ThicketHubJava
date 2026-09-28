package thickethub.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import thickethub.domain.enums.Papel;

public record UsuarioLoteItemDTO(
        @NotBlank String nome,
        @NotBlank @Email String email,
        @NotBlank String setor,
        @NotNull Papel papel
) {}
