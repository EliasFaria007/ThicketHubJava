package thickethub.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
@RequiredArgsConstructor
public class RateLimiterService {

    private final StringRedisTemplate redisTemplate;

    // Fallback thread-safe em memória quando o Redis não estiver disponível
    private final ConcurrentHashMap<String, RateLimitBucket> memoryBuckets = new ConcurrentHashMap<>();

    private static class RateLimitBucket {
        private final AtomicInteger count = new AtomicInteger(0);
        private final long resetTimeMillis;

        RateLimitBucket(long windowMillis) {
            this.resetTimeMillis = System.currentTimeMillis() + windowMillis;
        }

        boolean isExpired() {
            return System.currentTimeMillis() > resetTimeMillis;
        }

        int increment() {
            return count.incrementAndGet();
        }

        int get() {
            return count.get();
        }
    }

    /**
     * Tenta consumir uma permissão para a chave informada.
     * Retorna true se permitido, false se o limite foi atingido.
     * Utiliza Redis se conectado; caso contrário, ativa fallback transparente em memória.
     */
    public boolean permitir(String chave, int limite, Duration janela) {
        try {
            String key = "ratelimit:" + chave;
            Long contador = redisTemplate.opsForValue().increment(key);

            if (contador != null && contador == 1) {
                redisTemplate.expire(key, janela);
            }

            if (contador != null) {
                return contador <= limite;
            }
        } catch (Exception e) {
            log.debug("Redis indisponível para rate limiting (chave={}). Usando fallback em memória.", chave);
        }

        return permitirMemoria(chave, limite, janela);
    }

    private boolean permitirMemoria(String chave, int limite, Duration janela) {
        long windowMillis = Math.max(1000L, janela.toMillis());
        RateLimitBucket bucket = memoryBuckets.compute(chave, (k, existing) -> {
            if (existing == null || existing.isExpired()) {
                RateLimitBucket novo = new RateLimitBucket(windowMillis);
                novo.increment();
                return novo;
            }
            existing.increment();
            return existing;
        });

        // Limpeza preventiva periódica se o mapa crescer muito
        if (memoryBuckets.size() > 5000) {
            memoryBuckets.entrySet().removeIf(e -> e.getValue().isExpired());
        }

        return bucket.get() <= limite;
    }

    /**
     * Remove contadores de rate limit (ex: após login bem-sucedido).
     */
    public void limparRateLimit(String email, String ip) {
        if (email != null && !email.isBlank()) {
            String emailKey = "login:email:" + email.trim().toLowerCase();
            try {
                redisTemplate.delete("ratelimit:" + emailKey);
            } catch (Exception ignored) {}
            memoryBuckets.remove(emailKey);
        }

        if (ip != null && !ip.isBlank()) {
            String ipKey = "login:ip:" + ip.trim();
            try {
                redisTemplate.delete("ratelimit:" + ipKey);
            } catch (Exception ignored) {}
            memoryBuckets.remove(ipKey);
        }
    }

    /**
     * Variação específica para validação de OTP:
     * Limite de 3 tentativas por telefone em janela de 5 minutos.
     */
    public boolean permitirValidacaoOtp(String telefone) {
        return permitir("otp:" + telefone, 3, Duration.ofMinutes(5));
    }

    /**
     * Variação para tentativas de login por e-mail e por IP:
     * - Máximo 5 tentativas por e-mail a cada 15 minutos
     * - Máximo 10 tentativas por IP a cada 15 minutos
     */
    public boolean permitirLogin(String email, String ip) {
        String chaveIp = "login:ip:" + (ip != null && !ip.isBlank() ? ip.trim() : "127.0.0.1");
        boolean ipPermitido = permitir(chaveIp, 10, Duration.ofMinutes(15));

        boolean emailPermitido = true;
        if (email != null && !email.isBlank()) {
            String chaveEmail = "login:email:" + email.trim().toLowerCase();
            emailPermitido = permitir(chaveEmail, 5, Duration.ofMinutes(15));
        }

        return ipPermitido && emailPermitido;
    }
}
