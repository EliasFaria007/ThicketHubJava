package thickethub.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EncaminharChamadoRequest (
        Long tecnicoId,
        String filaSetor,
        @NotBlank @Size(min = 10, max = 1000) String motivo){}
