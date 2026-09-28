package thickethub.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record NovoChamadoRequest(
        @NotBlank @Size(min = 5, max = 200) String titulo,
        @NotBlank String descricao,
        @NotNull Long servicoId
) {
    public Long servicoIdLong() {
        return servicoId;
    }
}
