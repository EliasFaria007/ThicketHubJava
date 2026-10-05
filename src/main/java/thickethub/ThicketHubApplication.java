package thickethub;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@EnableScheduling
@SpringBootApplication
public class ThicketHubApplication {

    private static final AtomicBoolean frontEndConectado = new AtomicBoolean(false);

    public static void main(String[] args) {
        SpringApplication.run(ThicketHubApplication.class, args);
    }

    @Bean
    public ApplicationRunner avisoInicializacao() {
        return args -> {
            log.info("\n" +
                    "========================================================================================\n" +
                    "  🚀 THICKETHUB BACK-END INICIALIZADO COM SUCESSO!\n" +
                    "  📡 URL Base da API:       http://localhost:8080/api\n" +
                    "  📚 Swagger / OpenAPI UI:  http://localhost:8080/api/swagger-ui.html\n" +
                    "  💻 Front-End (Vite):      http://localhost:5173 [Pronto para conexão]\n" +
                    "  🔒 CORS configurado para: http://localhost:5173, http://localhost:3000\n" +
                    "========================================================================================");
        };
    }

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public Filter frontEndConnectionNotifierFilter() {
        return new Filter() {
            @Override
            public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
                    throws IOException, ServletException {
                if (request instanceof HttpServletRequest httpRequest) {
                    String origin = httpRequest.getHeader("Origin");
                    String referer = httpRequest.getHeader("Referer");
                    boolean ehFrontEnd = (origin != null && (origin.contains("5173") || origin.contains("3000") || origin.contains("thickethub")))
                            || (referer != null && (referer.contains("5173") || referer.contains("3000")));

                    if (ehFrontEnd) {
                        if (frontEndConectado.compareAndSet(false, true)) {
                            log.info("\n" +
                                    "****************************************************************************************\n" +
                                    "  🟢 [FRONT-END CONECTADO] O ThicketHubFront (http://localhost:5173) está rodando e conectado!\n" +
                                    "  📍 Primeira requisição recebida: {} {}\n" +
                                    "****************************************************************************************",
                                    httpRequest.getMethod(), httpRequest.getRequestURI());
                        } else {
                            log.debug("[FRONT-END ATIVO] Requisição do front-end: {} {} (Origin: {})",
                                    httpRequest.getMethod(), httpRequest.getRequestURI(), origin);
                        }
                    }
                }
                chain.doFilter(request, response);
            }
        };
    }
}
