package thickethub;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordTest {

    @Test
    void gerarHash() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);
        String hash = encoder.encode("Elefantamon1.");
        System.out.println("HASH_GERADO=" + hash);
        System.out.println("VALIDO=" + encoder.matches("Elefantamon1.", hash));
    }
}
