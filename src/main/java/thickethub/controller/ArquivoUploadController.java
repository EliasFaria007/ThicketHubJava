package thickethub.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import thickethub.dto.response.ApiResponse;
import thickethub.service.R2StorageService;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Controller para upload de arquivos (imagens e PDFs) para o Cloudflare R2.
 * Requer autenticação JWT — não está na lista de rotas públicas do SecurityConfig.
 */
@Slf4j
@RestController
@RequestMapping("/arquivos")
@RequiredArgsConstructor
public class ArquivoUploadController {

    /** Tamanho máximo permitido por arquivo: 10 MB */
    private static final long TAMANHO_MAXIMO_BYTES = 10L * 1024 * 1024;

    private final R2StorageService storageService;

    /**
     * Faz upload de um arquivo para o Cloudflare R2.
     *
     * @param arquivo Arquivo enviado (multipart/form-data)
     * @param pasta   Pasta de destino dentro do bucket (padrão: "anexos")
     * @return JSON com a URL pública do arquivo armazenado
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Map<String, String>>> upload(
            @RequestParam("arquivo") MultipartFile arquivo,
            @RequestParam(value = "pasta", defaultValue = "anexos") String pasta) throws IOException {

        if (arquivo.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.erro("O arquivo enviado está vazio.", List.of(), 400));
        }

        if (arquivo.getSize() > TAMANHO_MAXIMO_BYTES) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.erro("Arquivo muito grande. O tamanho máximo permitido é 10 MB.", List.of(), 400));
        }

        String url = storageService.uploadArquivo(arquivo, pasta);
        log.info("[UPLOAD] Arquivo salvo em R2: pasta={}, url={}", pasta, url);

        return ResponseEntity.ok(
                ApiResponse.sucesso(Map.of("url", url), "Upload realizado com sucesso", 200)
        );
    }
}
