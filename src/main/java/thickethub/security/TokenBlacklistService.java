package thickethub.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    private final StringRedisTemplate redisTemplate;
    private static final String PREFIXO = "blacklist:token:";

    /**
     * Circuit-breaker leve: emite WARN apenas uma vez quando o Redis fica
     * indisponível e silencia as ocorrências seguintes para DEBUG, evitando
     * inundar os logs em ambientes que não possuem Redis configurado.
     */
    private final AtomicBoolean redisDisponivel = new AtomicBoolean(true);
    private final AtomicBoolean avisoEmitido    = new AtomicBoolean(false);

    public void revogar(String token, Duration tempoRestante) {
        if (token == null || token.isBlank()) return;
        try {
            redisTemplate.opsForValue().set(PREFIXO + token, "revogado", tempoRestante);
            // Redis respondeu — reseta o circuit-breaker para emitir WARN novamente se cair
            redisDisponivel.set(true);
            avisoEmitido.set(false);
        } catch (Exception e) {
            registrarFalhaRedis("revogar token na blacklist", e);
        }
    }

    public boolean contem(String token) {
        if (token == null || token.isBlank()) return false;
        try {
            boolean resultado = Boolean.TRUE.equals(redisTemplate.hasKey(PREFIXO + token));
            redisDisponivel.set(true);
            avisoEmitido.set(false);
            return resultado;
        } catch (Exception e) {
            registrarFalhaRedis("consultar blacklist do Redis", e);
            return false;
        }
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private void registrarFalhaRedis(String operacao, Exception e) {
        redisDisponivel.set(false);
        if (avisoEmitido.compareAndSet(false, true)) {
            log.warn("Redis indisponível — operação '{}' será ignorada. "
                    + "Blacklist de tokens desativada até Redis reconectar. Causa: {}",
                    operacao, e.getMessage());
        } else {
            log.debug("Redis indisponível (já avisado) — operação '{}' ignorada: {}",
                    operacao, e.getMessage());
        }
    }
}
