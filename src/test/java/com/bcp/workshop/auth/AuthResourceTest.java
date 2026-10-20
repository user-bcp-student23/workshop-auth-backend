package com.bcp.workshop.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.junit.QuarkusTest;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;

@QuarkusTest
class AuthResourceTest {
    private static final String TEST_SECRET = "workshop-test-only-token-secret-32-bytes-minimum";

    @Test
    void logsInWithWorkshopUser() throws Exception {
        String token = given()
                .contentType("application/json")
                .body("{\"username\":\"student\",\"password\":\"Workshop123!\",\"client\":\"angular-workshop\"}")
                .when().post("/auth/login")
                .then()
                .statusCode(200)
                .body("user.username", is("student"))
                .extract().path("token");

        String[] segments = token.split("\\.");
        assertEquals(2, segments.length);
        Mac verifier = Mac.getInstance("HmacSHA256");
        verifier.init(new SecretKeySpec(TEST_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] expectedSignature = verifier.doFinal(segments[0].getBytes(StandardCharsets.US_ASCII));
        assertArrayEquals(expectedSignature, Base64.getUrlDecoder().decode(segments[1]));

        JsonNode claims = new ObjectMapper().readTree(Base64.getUrlDecoder().decode(segments[0]));
        assertEquals("student", claims.path("sub").asText());
        assertEquals("STUDENT", claims.path("role").asText());
        assertTrue(claims.path("exp").asLong() > Instant.now().getEpochSecond());
    }

    @Test
    void rejectsSqlSyntaxInPassword() {
        given()
                .contentType("application/json")
                .body("{\"username\":\"student\",\"password\":\"anything' OR '1'='1\",\"client\":\"angular-workshop\"}")
                .when().post("/auth/login")
                .then()
                .statusCode(401);
    }
}
