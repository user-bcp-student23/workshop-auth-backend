package com.bcp.workshop.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class LegacyTokenUtilTest {
    private static final String TEST_SECRET = "test-only-hmac-key-not-for-deployment";
    private static final long ISSUED_AT = 1_800_000_000L;

    @Test
    void createsVerifiableClaimsAndSignature() throws Exception {
        String client = " client-\"quoted\"-\\-\u00f1 ";
        LegacyTokenUtil.SignedToken issued = LegacyTokenUtil.create("student", client, TEST_SECRET, ISSUED_AT);
        String[] parts = issued.value().split("\\.", -1);
        assertEquals(2, parts.length);
        assertTrue(parts[0].matches("[A-Za-z0-9_-]+"));
        assertTrue(parts[1].matches("[A-Za-z0-9_-]+"));
        JsonNode claims = new ObjectMapper().readTree(Base64.getUrlDecoder().decode(parts[0]));
        assertEquals(4, claims.size());
        assertEquals("student", claims.path("sub").asText());
        assertEquals(client, claims.path("client").asText());
        assertTrue(claims.path("iat").isIntegralNumber());
        assertTrue(claims.path("exp").isIntegralNumber());
        assertEquals(ISSUED_AT, claims.path("iat").asLong());
        assertEquals(ISSUED_AT + 3600, claims.path("exp").asLong());
        Mac hmac = Mac.getInstance("HmacSHA256");
        hmac.init(new SecretKeySpec(TEST_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] expected = hmac.doFinal(parts[0].getBytes(StandardCharsets.US_ASCII));
        assertArrayEquals(expected, Base64.getUrlDecoder().decode(parts[1]));
        assertEquals(issued.value(), LegacyTokenUtil.create("student", client, TEST_SECRET, ISSUED_AT).value());
        assertEquals(ISSUED_AT, issued.issuedAt());
        assertEquals(ISSUED_AT + 3600, issued.expiresAt());
    }

    @Test
    void failsClosedWithoutExplicitKey() {
        for (String secret : new String[]{null, "", "  "}) {
            assertThrows(IllegalStateException.class,
                    () -> LegacyTokenUtil.create("student", "angular-workshop", secret, ISSUED_AT));
        }
    }

    @Test
    void pastExpirationIsExpiredAtControlledEvaluationTime() throws Exception {
        long evaluationTime = ISSUED_AT + 3601;
        String token = LegacyTokenUtil.create("student", "client", TEST_SECRET, ISSUED_AT).value();
        JsonNode claims = new ObjectMapper().readTree(Base64.getUrlDecoder().decode(token.split("\\.")[0]));
        assertTrue(claims.path("exp").asLong() < evaluationTime);
    }

    @Test
    void changingKeyChangesSignature() {
        String token = LegacyTokenUtil.create("student", "client", TEST_SECRET, ISSUED_AT).value();
        String other = LegacyTokenUtil.create("student", "client", "other-test-only-key", ISSUED_AT).value();
        assertNotEquals(token, other);
    }
}