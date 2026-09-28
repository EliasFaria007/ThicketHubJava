package thickethub.dto.response;

import java.util.List;

public record ApiResponse<T>(
        T data,
        String mensagem,
        List<String> erros,
        int status
) {
    
    public static <T> ApiResponse<T> sucesso(T data, String mensagem, int status) {
        return new ApiResponse<>(data, mensagem, List.of(), status);
    }

    public static <T> ApiResponse<T> erro(String mensagem, List<String> erros, int status) {
        return new ApiResponse<>(null, mensagem, erros, status);
    }
}
