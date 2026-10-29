package thickethub.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

import java.net.URI;

/**
 * Configuração do cliente S3 compatível com Cloudflare R2.
 * O R2 é 100% compatível com a API do AWS S3, portanto utilizamos o SDK da AWS.
 */
@Configuration
public class StorageConfig {

    @Bean
    public S3Client s3Client(
            @Value("${storage.r2.endpoint}") String endpoint,
            @Value("${storage.r2.access-key}") String accessKey,
            @Value("${storage.r2.secret-key}") String secretKey) {

        return S3Client.builder()
                .endpointOverride(URI.create(endpoint))
                // O R2 exige a região "auto" para roteamento correto
                .region(Region.of("auto"))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)))
                .build();
    }
}
