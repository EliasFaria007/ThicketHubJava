package thickethub.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import thickethub.domain.model.Usuario;
import thickethub.exception.UnauthorizedException;

public final class SecurityUtils {

    private SecurityUtils() {}

    public static String getEmailUsuarioLogado() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new UnauthorizedException("Usuário não autenticado.");
        }
        return auth.getName();
    }

    public static Usuario getUsuarioLogado() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
            return principal.getUsuario();
        }
        throw new UnauthorizedException("Usuário autenticado não encontrado no contexto.");
    }
}
