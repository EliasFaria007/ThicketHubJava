package thickethub.security;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class RateLimiterService {

    private final StringRedisTemplate redisTemplate;

    /**
     * Tenta consumir uma "permissão" para a chave informada.
     * Retorna true se permitido, false se o limite foi atingido.
     */
    public boolean permitir(String chave, int limite, Duration janela) {
        String key = "ratelimit:" + chave;
        Long contador = redisTemplate.opsForValue().increment(key);   // INCR (atômico)

        if (contador != null && contador == 1) {
            redisTemplate.expire(key, janela);                        // EXPIRE (janela)
        }

        return contador != null && contador <= limite;
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