package thickethub.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import thickethub.repository.ConfiguracaoSistemaRepository;
import thickethub.service.ConfiguracaoGeralService;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Implementação de {@link ConfiguracaoGeralService} que lê as configurações
 * gerais do sistema a partir da tabela {@code tb_configuracao_sistema}.
 * <p>
 * Valores padrão são retornados quando o banco não possui o registro.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConfiguracaoGeralServiceImpl implements ConfiguracaoGeralService {

    private static final String CHAVE_GERAL = "geral";

    private final ConfiguracaoSistemaRepository configuracaoSistemaRepository;
    private final ObjectMapper objectMapper;

    @Override
    public Map<String, Object> obterConfigGeral() {
        return configuracaoSistemaRepository.findById(CHAVE_GERAL)
                .map(cfg -> {
                    try {
                        return (Map<String, Object>) objectMapper.readValue(
                                cfg.getValor(),
                                new TypeReference<LinkedHashMap<String, Object>>() {}
                        );
                    } catch (Exception e) {
                        log.warn("[CONFIG GERAL] Falha ao deserializar configurações gerais — usando defaults. Causa: {}", e.getMessage());
                        return defaults();
                    }
                })
                .orElseGet(this::defaults);
    }

    @Override
    public boolean getBoolean(String chave, boolean valorPadrao) {
        Object val = obterConfigGeral().get(chave);
        if (val instanceof Boolean b) return b;
        if (val instanceof String s) return Boolean.parseBoolean(s);
        return valorPadrao;
    }

    @Override
    public int getInt(String chave, int valorPadrao) {
        Object val = obterConfigGeral().get(chave);
        if (val instanceof Number n) return n.intValue();
        if (val instanceof String s) {
            try { return Integer.parseInt(s); } catch (NumberFormatException ignored) {}
        }
        return valorPadrao;
    }

    @Override
    public String getString(String chave, String valorPadrao) {
        Object val = obterConfigGeral().get(chave);
        if (val instanceof String s && !s.isBlank()) return s;
        return valorPadrao;
    }

    // Valores padrão (espelho dos defaults em ConfiguracaoController)
    private Map<String, Object> defaults() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("justificativaNotaBaixaObrigatoria", true);
        m.put("alertaNotaBaixaAtivo", true);
        m.put("fechamentoInatividadeAtivo", true);
        m.put("diasInatividadeFechamento", 5);
        m.put("webhookAtivo", false);
        m.put("webhookUrl", "");
        m.put("webhookNivelMinimo", "CRITICA");
        return m;
    }
}
