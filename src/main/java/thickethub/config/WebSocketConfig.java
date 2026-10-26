package thickethub.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import thickethub.security.JwtService;

import java.security.Principal;

@Slf4j
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtService jwtService;

    private static final String[] ORIGENS_PERMITIDAS = {
            "https://*.thickethub.dev.br",
            "https://thickethub.dev.br",
            "https://*thickethub*.vercel.app",
            "https://thicket-hub-front.vercel.app",
            "http://localhost:*",
            "http://127.0.0.1:*"
    };

    /**
     * Endpoint de handshake:
     * Registra endpoint nativo para 'new WebSocket("wss://...")'
     * e com fallback SockJS para clientes que utilizam a biblioteca SockJS.
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns(ORIGENS_PERMITIDAS);

        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns(ORIGENS_PERMITIDAS)
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

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor =
                        MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

                if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
                    String authHeader = accessor.getFirstNativeHeader("Authorization");
                    String passcode = accessor.getPasscode();
                    String token = null;

                    if (authHeader != null && authHeader.startsWith("Bearer ")) {
                        token = authHeader.substring(7);
                    } else if (authHeader != null && !authHeader.isBlank()) {
                        token = authHeader;
                    } else if (passcode != null && !passcode.isBlank()) {
                        token = passcode;
                    }

                    if (token != null) {
                        try {
                            String email = jwtService.extractUsername(token);
                            if (email != null) {
                                Principal principal = () -> email;
                                accessor.setUser(principal);
                                log.info("WebSocket STOMP autenticado com sucesso para: {}", email);
                            }
                        } catch (Exception e) {
                            log.warn("Falha ao autenticar token no WebSocket: {}", e.getMessage());
                        }
                    }
                }
                return message;
            }
        });
    }
}