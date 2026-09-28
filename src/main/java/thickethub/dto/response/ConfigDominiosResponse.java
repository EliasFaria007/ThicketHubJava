package thickethub.dto.response;

import java.util.List;

public record ConfigDominiosResponse(
        Long id,
        String dominio,
        Boolean ehPadrao,
        Boolean ativo
) {
    public static ConfigDominiosResponse de(thickethub.domain.model.ConfigDominio c) {
        if (c == null) return null;
        return new ConfigDominiosResponse(c.getId(), c.getDominio(), c.getEhPadrao(), c.getAtivo());
    }
}
