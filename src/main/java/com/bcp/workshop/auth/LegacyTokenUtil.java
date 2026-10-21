package com.bcp.workshop.auth;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

public final class LegacyTokenUtil {
    private static final long TOKEN_LIFETIME_SECONDS = 3600;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public record SignedToken(String value, long issuedAt, long expiresAt) {}

    private LegacyTokenUtil() {}

    public static SignedToken create(String username, String client, String secret, long issuedAt) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("Token signing key is not configured");
        }
        long expiresAt = Math.addExact(issuedAt, TOKEN_LIFETIME_SECONDS);
        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("sub", username);
        claims.put("client", client);
        claims.put("iat", issuedAt);
        claims.put("exp", expiresAt);
        try {
            String payload = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(OBJECT_MAPPER.writeValueAsBytes(claims));
            Mac hmac = Mac.getInstance("HmacSHA256");
            hmac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            String signature = Base64.getUrlEncoder().withoutPadding().encodeToString(
                    hmac.doFinal(payload.getBytes(StandardCharsets.US_ASCII)));
            return new SignedToken(payload + "." + signature, issuedAt, expiresAt);
        } catch (JsonProcessingException | GeneralSecurityException error) {
            throw new IllegalStateException("Token signing failed");
        }
    }
}
