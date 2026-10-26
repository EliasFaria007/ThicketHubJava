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

                Map<String, String> queryParams = new java.util.LinkedHashMap<>();
                if (query != null && !query.isBlank()) {
                    for (String part : query.split("&")) {
                        String[] kv = part.split("=", 2);
                        if (kv.length == 2) {
                            queryParams.put(kv[0], kv[1]);
                        } else if (kv.length == 1 && !kv[0].isBlank()) {
                            queryParams.put(kv[0], "");
                        }
                    }
                }

                // Parâmetros críticos para Neon Serverless e PgBouncer (Transaction Pooling)
                queryParams.putIfAbsent("sslmode", "require");
                queryParams.putIfAbsent("prepareThreshold", "0");
                queryParams.putIfAbsent("preparedStatementCacheQueries", "0");
                queryParams.putIfAbsent("tcpKeepAlive", "true");
                queryParams.putIfAbsent("connectTimeout", "10");
                queryParams.putIfAbsent("socketTimeout", "45");

                StringBuilder queryStr = new StringBuilder();
                for (Map.Entry<String, String> entry : queryParams.entrySet()) {
                    if (!queryStr.isEmpty()) queryStr.append("&");
                    queryStr.append(entry.getKey()).append("=").append(entry.getValue());
                }

                StringBuilder jdbcUrl = new StringBuilder("jdbc:postgresql://")
                        .append(host)
                        .append(":")
                        .append(port)
                        .append(path)
                        .append("?")
                        .append(queryStr);

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
