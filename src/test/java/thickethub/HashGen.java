package thickethub;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class HashGen {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);
        String hash = encoder.encode("Elefantamon1.");
        System.out.println("BCRYPT_RESULT:" + hash);
    }
}
