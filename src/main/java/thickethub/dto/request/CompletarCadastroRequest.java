package thickethub.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CompletarCadastroRequest(
        @NotBlank @Size(min = 6, max = 100) String novaSenha,
        String telefone,
        String setor,
        String localidade
) {}
