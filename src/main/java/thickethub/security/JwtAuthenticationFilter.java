package thickethub.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final TokenBlacklistService tokenBlacklistService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String metodo = request.getMethod();
        String uri = request.getRequestURI();
        log.debug("[JWT FILTER] Requisição recebida: {} {}", metodo, uri);

        String header = request.getHeader("Authorization");

        if (header == null || !header.startsWith("Bearer ")) {
            log.debug("[JWT FILTER] Sem token Bearer na requisição {} {} — seguindo sem autenticação", metodo, uri);
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(7).trim();

        if (tokenBlacklistService.contem(token)) {
            log.warn("[JWT FILTER] Token revogado (na blacklist) usado na requisição {} {}", metodo, uri);
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String email = jwtService.extractUsername(token);
            log.debug("[JWT FILTER] Token extraído com sucesso — usuário: {}", email);

            if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(email);

                if (jwtService.isTokenValid(token, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails, null, userDetails.getAuthorities());
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    log.debug("[JWT FILTER] Usuário '{}' autenticado com sucesso para {} {}", email, metodo, uri);
                } else {
                    log.warn("[JWT FILTER] Token inválido para o usuário '{}' na requisição {} {}", email, metodo, uri);
                }
            }
        } catch (Exception e) {
            log.warn("[JWT FILTER] Falha ao processar token JWT na requisição {} {}: {}", metodo, uri, e.getMessage());
        }

        filterChain.doFilter(request, response);
    }
}