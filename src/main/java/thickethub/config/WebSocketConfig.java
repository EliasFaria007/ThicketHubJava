package thickethub.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    /**
     * Endpoint de handshake: é por aqui que o frontend "sobe" a conexão.
     * SockJS é o fallback para navegadores/redes que bloqueiam WebSocket puro.
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns(
                        "https://thickethub.defensoria.mg.gov.br",
                        "http://localhost:5173")
                .withSockJS();
    }

    /**
     * Broker de mensagens:
     *  - /topic/**  → broadcast (todos os inscritos recebem)
     *  - /queue/**  → mensagens privadas, roteadas por usuário via /user
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }
}