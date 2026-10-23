package thickethub.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Normaliza automaticamente URLs de banco fornecidas por provedores em nuvem (Northflank, Neon, Supabase, etc.)
 * que comumente injetam 'DATABASE_URL' ou 'POSTGRES_URI' no formato postgres:// ou postgresql://.
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
public class DatabaseUrlEnvironmentPostProcessor implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String dbUrl = environment.getProperty("DATABASE_URL");
        if (dbUrl == null || dbUrl.isBlank()) {
            dbUrl = environment.getProperty("POSTGRES_URI");
        }

        if (dbUrl == null || dbUrl.isBlank()) {
            return;
        }

        if (dbUrl.startsWith("postgres://") || dbUrl.startsWith("postgresql://")) {
            try {
                // Converte temporariamente o protocolo para http apenas para validação sintática do URI
                String sanitized = dbUrl.replaceFirst("^(postgres|postgresql)://", "http://");
                URI uri = new URI(sanitized);

                String host = uri.getHost();
                int port = uri.getPort() == -1 ? 5432 : uri.getPort();
                String path = uri.getPath() != null && !uri.getPath().isEmpty() ? uri.getPath() : "/thickethub_db";
                String query = uri.getQuery();

                StringBuilder jdbcUrl = new StringBuilder("jdbc:postgresql://")
                        .append(host)
                        .append(":")
                        .append(port)
                        .append(path);

                if (query != null && !query.isBlank()) {
                    jdbcUrl.append("?").append(query);
                } else {
                    jdbcUrl.append("?sslmode=require");
                }

                Map<String, Object> props = new HashMap<>();
                props.put("spring.datasource.url", jdbcUrl.toString());

                if (uri.getUserInfo() != null && !uri.getUserInfo().isBlank()) {
                    String[] userInfo = uri.getUserInfo().split(":", 2);
                    props.put("spring.datasource.username", URLDecoder.decode(userInfo[0], StandardCharsets.UTF_8));
                    if (userInfo.length > 1) {
                        props.put("spring.datasource.password", URLDecoder.decode(userInfo[1], StandardCharsets.UTF_8));
                    }
                }

                environment.getPropertySources().addFirst(new MapPropertySource("cloudDatabaseUrlConfig", props));
            } catch (Exception e) {
                // Silently fallback se não conseguir processar o URI
            }
        }
    }
}
