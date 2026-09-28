package thickethub.dto.response;

import thickethub.domain.enums.Papel;

import java.util.Set;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        Long id,
        String nome,
        String email,
        Papel papel,
        String setor,
        Boolean primeiroAcessoConcluido,
        Set<String> setoresLiberados
) {}
