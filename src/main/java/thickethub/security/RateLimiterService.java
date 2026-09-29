package thickethub.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class RateLimiterService {

    private final StringRedisTemplate redisTemplate;

    /**
     * Tenta consumir uma "permissão" para a chave informada.
     * Retorna true se permitido, false se o limite foi atingido.
     * Em caso de falha na conexão com Redis, permite a requisição (fail-open).
     */
    public boolean permitir(String chave, int limite, Duration janela) {
        try {
            String key = "ratelimit:" + chave;
            Long contador = redisTemplate.opsForValue().increment(key);   // INCR (atômico)

            if (contador != null && contador == 1) {
                redisTemplate.expire(key, janela);                        // EXPIRE (janela)
            }

            return contador != null && contador <= limite;
        } catch (Exception e) {
            log.warn("Redis indisponível para rate limiting (chave={}). Permitindo requisição. Erro: {}", chave, e.getMessage());
            return true; // fail-open: permite quando Redis está fora
        }
    }

    /**
     * Variação específica para validação de OTP:
     * limite fixo de 3 tentativas, alinhado com o contadorTentativas
     * da entidade RecuperacaoSms.
     */
    public boolean permitirValidacaoOtp(String telefone) {
        return permitir("otp:" + telefone, 3, Duration.ofMinutes(5));
    }

    /**
     * Variação para tentativas de login por e-mail e por IP.
     */
    public boolean permitirLogin(String email, String ip) {
        return permitir("login:email:" + email, 5, Duration.ofMinutes(15))
               && permitir("login:ip:" + ip, 20, Duration.ofMinutes(15));
    }
}