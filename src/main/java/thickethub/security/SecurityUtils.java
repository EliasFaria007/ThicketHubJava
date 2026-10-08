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

    public static String obterIpCliente() {
        try {
            org.springframework.web.context.request.RequestAttributes attribs =
                    org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
            if (attribs instanceof org.springframework.web.context.request.ServletRequestAttributes servletAttributes) {
                jakarta.servlet.http.HttpServletRequest req = servletAttributes.getRequest();
                return extrairIp(req);
            }
        } catch (Exception ignored) {}
        return "127.0.0.1";
    }

    public static String extrairIp(jakarta.servlet.http.HttpServletRequest req) {
        if (req == null) return "127.0.0.1";
        String[] headers = {
                "X-Forwarded-For",
                "X-Real-IP",
                "CF-Connecting-IP",
                "Proxy-Client-IP",
                "WL-Proxy-Client-IP",
                "HTTP_CLIENT_IP",
                "HTTP_X_FORWARDED_FOR"
        };
        for (String header : headers) {
            String ip = req.getHeader(header);
            if (ip != null && !ip.isBlank() && !"unknown".equalsIgnoreCase(ip)) {
                return ip.split(",")[0].trim();
            }
        }
        String remoteAddr = req.getRemoteAddr();
        if ("0:0:0:0:0:0:0:1".equals(remoteAddr) || "::1".equals(remoteAddr)) {
            return "127.0.0.1";
        }
        return remoteAddr != null && !remoteAddr.isBlank() ? remoteAddr : "127.0.0.1";
    }
}
