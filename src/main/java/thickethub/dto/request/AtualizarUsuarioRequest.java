package thickethub.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import thickethub.domain.enums.Papel;

import java.util.Set;

public record AtualizarUsuarioRequest(
        String nome,
        @Email String email,
        Papel papel,
        String setor,
        String telefone,
        String localidade,
        Boolean ativo,
        Boolean podeCadastrarUsuarios,
        Set<String> setoresLiberados,
        String assinatura,
        Boolean incluirAssinaturaPadrao,
        String termosVersao,
        Boolean primeiroAcessoConcluido
) {}
