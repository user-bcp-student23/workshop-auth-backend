package com.bcp.workshop.auth;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.eclipse.microprofile.config.ConfigProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.Base64;

public final class LegacyTokenUtil {
    private static final String SECRET_PROPERTY = "auth.token.secret";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private LegacyTokenUtil() {}

    public static String create(String username, String role) {
        try {
            String secret = ConfigProvider.getConfig().getValue(SECRET_PROPERTY, String.class);
            byte[] key = secret.getBytes(StandardCharsets.UTF_8);
            if (key.length < 32) {
                throw new IllegalStateException("AUTH_TOKEN_SECRET must contain at least 32 bytes");
            }
            long expiry = Instant.now().plusSeconds(3600).getEpochSecond();
            byte[] claims = OBJECT_MAPPER.writeValueAsBytes(Map.of("sub", username, "role", role, "exp", expiry));
            String payload = Base64.getUrlEncoder().withoutPadding().encodeToString(claims);
            Mac signer = Mac.getInstance("HmacSHA256");
            signer.init(new SecretKeySpec(key, "HmacSHA256"));
            String signature = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(signer.doFinal(payload.getBytes(StandardCharsets.US_ASCII)));
            return payload + "." + signature;
        } catch (IllegalStateException error) {
            throw error;
        } catch (Exception error) {
            throw new IllegalStateException("Unable to issue authentication token", error);
        }
    }
}
