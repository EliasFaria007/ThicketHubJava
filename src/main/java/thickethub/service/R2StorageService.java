package thickethub.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Serviço de armazenamento de arquivos no Cloudflare R2.
 * Responsável por fazer upload e remoção de arquivos (imagens e PDFs).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class R2StorageService {

    private static final Set<String> TIPOS_PERMITIDOS = Set.of(
            "image/png", "image/jpeg", "image/webp", "image/gif", "application/pdf"
    );

    private final S3Client s3Client;

    @Value("${storage.r2.bucket-name}")
    private String bucketName;

    @Value("${storage.r2.public-url}")
    private String publicUrl;

    /**
     * Faz upload de um arquivo para o Cloudflare R2.
     *
     * @param arquivo Arquivo recebido pelo endpoint (multipart)
     * @param pasta   Pasta/prefixo dentro do bucket (ex: "assinaturas", "anexos")
     * @return URL pública do arquivo armazenado
     * @throws IOException              se ocorrer erro ao ler o arquivo
     * @throws IllegalArgumentException se o tipo do arquivo não for permitido
     */
    public String uploadArquivo(MultipartFile arquivo, String pasta) throws IOException {
        validarTipoArquivo(arquivo);

        String nomeOriginal = Objects.requireNonNull(arquivo.getOriginalFilename(), "Nome do arquivo é obrigatório");
        String extensao = obterExtensao(nomeOriginal);
        String chaveUnica = pasta + "/" + UUID.randomUUID() + extensao;

        PutObjectRequest putRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(chaveUnica)
                .contentType(arquivo.getContentType())
                .contentLength(arquivo.getSize())
                .build();

        s3Client.putObject(putRequest, RequestBody.fromInputStream(arquivo.getInputStream(), arquivo.getSize()));

        String urlPublica = publicUrl + "/" + chaveUnica;
        log.info("[R2 STORAGE] Upload concluído: {}", urlPublica);
        return urlPublica;
    }

    /**
     * Remove um arquivo do Cloudflare R2 a partir da sua URL pública.
     *
     * @param urlPublica URL pública do arquivo a ser removido
     */
    /**
     * Faz upload de bytes brutos para o Cloudflare R2 (usado por anexos de e-mail inbound).
     *
     * @param bytes        Conteúdo binário do arquivo
     * @param nomeOriginal Nome original do arquivo (para obter a extensão correta)
     * @param contentType  MIME type do arquivo
     * @param pasta        Pasta/prefixo dentro do bucket (ex: "anexos")
     * @return URL pública do arquivo armazenado ou null se houver falha
     */
    public String uploadBytes(byte[] bytes, String nomeOriginal, String contentType, String pasta) {
        try {
            if (s3Client == null || bucketName == null || bucketName.isBlank()) {
                log.warn("[R2 STORAGE] Cliente S3 não inicializado ou bucket ausente");
                return null;
            }
            String extensao = obterExtensao(nomeOriginal != null ? nomeOriginal : "arquivo.png");
            String chaveUnica = pasta + "/" + UUID.randomUUID() + extensao;

            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(chaveUnica)
                    .contentType(contentType != null ? contentType : "application/octet-stream")
                    .contentLength((long) bytes.length)
                    .build();

            s3Client.putObject(putRequest, RequestBody.fromBytes(bytes));

            String urlPublica = (publicUrl != null && !publicUrl.isBlank())
                    ? (publicUrl.endsWith("/") ? publicUrl + chaveUnica : publicUrl + "/" + chaveUnica)
                    : chaveUnica;
            log.info("[R2 STORAGE] Upload de bytes concluído: {}", urlPublica);
            return urlPublica;
        } catch (Exception e) {
            log.warn("[R2 STORAGE] Falha ao fazer upload de bytes para R2: {}", e.getMessage());
            return null;
        }
    }

    public void deletarArquivo(String urlPublica) {
        if (urlPublica == null || !urlPublica.startsWith(publicUrl)) {
            log.warn("[R2 STORAGE] URL inválida para remoção: {}", urlPublica);
            return;
        }
        String chave = urlPublica.replace(publicUrl + "/", "");
        s3Client.deleteObject(DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(chave)
                .build());
        log.info("[R2 STORAGE] Arquivo removido: {}", chave);
    }

    // ──────────────────────────────────────────────────────────
    // Métodos auxiliares privados
    // ──────────────────────────────────────────────────────────

    private void validarTipoArquivo(MultipartFile arquivo) {
        String contentType = arquivo.getContentType();
        if (contentType == null || !TIPOS_PERMITIDOS.contains(contentType)) {
            throw new IllegalArgumentException(
                    "Tipo de arquivo não permitido: " + contentType +
                    ". Tipos aceitos: PNG, JPEG, WEBP, GIF, PDF."
            );
        }
    }

    private String obterExtensao(String nomeOriginal) {
        int indicePonto = nomeOriginal.lastIndexOf(".");
        return indicePonto >= 0 ? nomeOriginal.substring(indicePonto).toLowerCase() : "";
    }
}
