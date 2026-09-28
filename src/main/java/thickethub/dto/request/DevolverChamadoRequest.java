package thickethub.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DevolverChamadoRequest (
       @NotBlank @Size(min = 3) String motivo){}
