package thickethub.service;

public interface EmailService {
    void enviarEmail(String destinatario, String assunto, String corpo);
}
