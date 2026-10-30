package thickethub.service;

public interface EmailService {
    void enviarEmail(String destinatario, String assunto, String corpo);
    void enviarEmailHtml(String destinatario, String assunto, String html, String textoFallback);
    boolean enviarEmailSincrono(String destinatario, String assunto, String corpo);

    void enviarNotificacaoChamado(
            String destinatario,
            String nomeDestinatario,
            String protocolo,
            String titulo,
            String status,
            String prioridade,
            String setor,
            String tecnicoNome,
            Long chamadoId,
            String mensagemDestaque
    );

    void enviarCodigoRecuperacao(
            String destinatario,
            String nomeDestinatario,
            String codigoOtp,
            int minutosValidade
    );
}
