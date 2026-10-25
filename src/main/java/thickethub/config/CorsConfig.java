package thickethub.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Configuration
public class CorsConfig {

    @Value("${cors.allowed-origins:}")
    private String allowedOriginsEnv;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        List<String> origins = new ArrayList<>(List.of(
                "https://thickethub.defensoria.mg.gov.br",
                "https://thickethub.dev.br",
                "https://www.thickethub.dev.br",
                "https://thicket-hub-front.vercel.app",
                "http://localhost:5173",
                "http://localhost:3000"
        ));

        if (allowedOriginsEnv != null && !allowedOriginsEnv.isBlank()) {
            Arrays.stream(allowedOriginsEnv.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty() && !"*".equals(s))
                    .forEach(origins::add);
        }

        // Origens estritas permitidas (sem wildcard aberto para evitar roubo de sessão via CORS)
        config.setAllowedOrigins(origins);
        config.setAllowedOriginPatterns(List.of(
                "https://*.thickethub.dev.br",
                "https://*thickethub*.vercel.app",
                "http://localhost:[*]",
                "http://127.0.0.1:[*]"
        ));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "HEAD"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", "X-Requested-With", "Origin", "Access-Control-Request-Method", "Access-Control-Request-Headers"));
        config.setExposedHeaders(List.of("Authorization", "Content-Disposition"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}