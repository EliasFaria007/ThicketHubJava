package thickethub.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    private final StringRedisTemplate redisTemplate;
    private static final String PREFIXO = "blacklist:token:";

    public void revogar(String token, Duration tempoRestante) {
        if (token == null || token.isBlank()) return;
        try {
            redisTemplate.opsForValue().set(PREFIXO + token, "revogado", tempoRestante);
        } catch (Exception e) {
            log.warn("Falha ao registrar token na blacklist do Redis: {}", e.getMessage());
        }
    }

    public boolean contem(String token) {
        if (token == null || token.isBlank()) return false;
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(PREFIXO + token));
        } catch (Exception e) {
            log.warn("Falha ao consultar blacklist do Redis: {}", e.getMessage());
            return false;
        }
    }
}
