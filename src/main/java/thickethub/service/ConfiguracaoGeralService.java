package thickethub.service;

import java.util.Map;

/**
 * Serviço responsável por fornecer acesso às configurações gerais do sistema
 * (ex: fechamento por inatividade, webhook, alertas de nota baixa).
 * <p>
 * As configurações são lidas da tabela {@code tb_configuracao_sistema} no banco de dados.
 */
public interface ConfiguracaoGeralService {

    /**
     * Retorna o mapa de configurações gerais.
     * Caso não haja registro no banco, retorna os valores padrão.
     */
    Map<String, Object> obterConfigGeral();

    /**
     * Retorna o valor booleano de uma chave de configuração geral.
     *
     * @param chave        chave do mapa de configuração
     * @param valorPadrao  valor padrão caso a chave não exista
     */
    boolean getBoolean(String chave, boolean valorPadrao);

    /**
     * Retorna o valor inteiro de uma chave de configuração geral.
     *
     * @param chave        chave do mapa de configuração
     * @param valorPadrao  valor padrão caso a chave não exista
     */
    int getInt(String chave, int valorPadrao);

    /**
     * Retorna o valor String de uma chave de configuração geral.
     *
     * @param chave        chave do mapa de configuração
     * @param valorPadrao  valor padrão caso a chave não exista
     */
    String getString(String chave, String valorPadrao);
}
