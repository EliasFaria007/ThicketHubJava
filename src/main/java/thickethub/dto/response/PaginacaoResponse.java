package thickethub.dto.response;

import org.springframework.data.domain.Page;

import java.util.List;

public record PaginacaoResponse<T>(
        List<T> items,
        long total,
        int page,
        int limit,
        int totalPages
) {

    public static <T> PaginacaoResponse<T> de(Page<T> page) {
        return new PaginacaoResponse<>(
                page.getContent(),
                page.getTotalElements(),
                page.getNumber(),
                page.getSize(),
                page.getTotalPages()
        );
    }
}