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
                "http://localhost:5173",
                "http://localhost:3000",
                "https://thicket-hub-front.vercel.app",
                "https://*.vercel.app",
                "https://*.northflank.app",
                "https://*.code.run",
                "http://*.code.run",
                "https://thickethub.dev.br",
                "https://www.thickethub.dev.br"
        ));

        if (allowedOriginsEnv != null && !allowedOriginsEnv.isBlank()) {
            if ("*".equals(allowedOriginsEnv.trim())) {
                origins = List.of("*");
            } else {
                Arrays.stream(allowedOriginsEnv.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .forEach(origins::add);
            }
        } else {
            // Em nuvem (Vercel, Northflank, etc.), permite qualquer origem dinamicamente com credenciais
            origins.add("*");
        }

        config.setAllowedOriginPatterns(origins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "HEAD"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("Authorization", "Content-Disposition"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
