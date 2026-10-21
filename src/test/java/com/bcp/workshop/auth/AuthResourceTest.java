package com.bcp.workshop.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import org.eclipse.microprofile.config.ConfigProvider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThanOrEqualTo;

@QuarkusTest
class AuthResourceTest {
    @Test
    void logsInWithWorkshopUser() {
        given()
                .contentType("application/json")
                .body("{\"username\":\"student\",\"password\":\"Workshop123!\",\"client\":\"angular-workshop\"}")
                .when().post("/auth/login")
                .then()
                .statusCode(200)
                .body("user.username", is("student"))
                .body("user.displayName", is("Workshop Student"))
                .body("user.role", is("STUDENT"))
                .body("expiresIn", both(greaterThanOrEqualTo(3599)).and(lessThanOrEqualTo(3600)))
                .body("debug", is("authenticated-with-legacy-flow"))
                .body("token", notNullValue());
    }

    @Test
    void rejectsMissingNullEmptyOrBlankClient() {
        assertLoginError("{\"username\":\"student\",\"password\":\"Workshop123!\"}", 400, "Client is required");
        for (String client : new String[]{"null", "\"\"", "\"  \"", "\"\\t\\n\""}) {
            assertLoginError("{\"username\":\"student\",\"password\":\"Workshop123!\",\"client\":" + client + "}",
                    400, "Client is required");
        }
    }

    @Test
    void rejectsNullRequestWithBadRequest() {
        assertLoginError("null", 400, "Request is null");
    }

    @Test
    void rejectsMissingOrBlankUsernameWithBadRequest() {
        assertLoginError("{\"password\":\"Workshop123!\"}", 400, "Username is required");
        assertLoginError("{\"username\":\"  \",\"password\":\"Workshop123!\"}", 400, "Username is required");
    }

    @Test
    void rejectsMissingOrBlankPasswordWithBadRequest() {
        assertLoginError("{\"username\":\"student\"}", 400, "Password is required");
        assertLoginError("{\"username\":\"student\",\"password\":\"  \"}", 400, "Password is required");
    }

    @Test
    void rejectsInvalidCredentialsWithUnauthorized() {
        assertLoginError("{\"username\":\"unknown\",\"password\":\"Workshop123!\",\"client\":\"angular-workshop\"}", 401,
                "Invalid user unknown or password");
        assertLoginError("{\"username\":\"student\",\"password\":\"invalid\",\"client\":\"angular-workshop\"}", 401,
                "Invalid user student or password");
    }

    @Test
    void preservesClientAndReportsConsistentExpiry() throws Exception {
        String client = "  browser-\"quoted\"  ";
        var response = given()
                .contentType("application/json")
                .body(Map.of("username", "student", "password", "Workshop123!", "client", client))
                .when().post("/auth/login")
                .then().statusCode(200)
                .body("user.username", is("student"))
                .body("user.displayName", is("Workshop Student"))
                .body("user.role", is("STUDENT"))
                .body("debug", is("authenticated-with-legacy-flow"))
                .extract().response();
        String token = response.path("token");
        JsonNode claims = new ObjectMapper().readTree(Base64.getUrlDecoder().decode(token.split("\\.")[0]));
        assertEquals(client, claims.path("client").asText());
        assertEquals("student", claims.path("sub").asText());
        assertEquals(3600, claims.path("exp").asLong() - claims.path("iat").asLong());
        long remaining = claims.path("exp").asLong() - Instant.now().getEpochSecond();
        assertTrue(Math.abs(response.<Integer>path("expiresIn") - remaining) <= 1);
    }

    @Test
    void authenticationLogsExcludeSensitiveValues() {
        var messages = new CopyOnWriteArrayList<String>();
        Handler capture = new Handler() {
            private final SimpleFormatter formatter = new SimpleFormatter();

            @Override
            public void publish(LogRecord record) {
                messages.add(formatter.format(record));
            }

            @Override
            public void flush() {}

            @Override
            public void close() {}
        };
        Logger root = Logger.getLogger("");
        root.addHandler(capture);
        try {
            String password = "Workshop123!";
            String invalidPassword = "invalid-password-log-marker";
            String token = given().contentType("application/json")
                    .body(Map.of("username", "student", "password", password, "client", "browser"))
                    .when().post("/auth/login")
                    .then().statusCode(200).extract().path("token");
            given().contentType("application/json")
                    .body(Map.of("username", "student", "password", invalidPassword, "client", "browser"))
                    .when().post("/auth/login").then().statusCode(401);
            given().contentType("application/json")
                    .body(Map.of("username", "student", "password", password, "client", " "))
                    .when().post("/auth/login").then().statusCode(400);
            String probe = "log-capture-probe";
            Logger.getLogger(AuthResource.class.getName()).info(probe);
            String logs = String.join("\n", messages);
            assertTrue(logs.contains(probe));
            String secret = ConfigProvider.getConfig().getValue("AUTH_TOKEN_SECRET", String.class);
            for (String sensitive : new String[]{token, secret, password, invalidPassword}) {
                assertFalse(logs.contains(sensitive), "Sensitive authentication value found in logs");
            }
        } finally {
            root.removeHandler(capture);
            capture.close();
        }
    }

    @Test
    void doesNotIssueTokenWhenKeyIsMissing() {
        AuthResource resource = new AuthResource();
        resource.database = new LegacyDatabase() {
            @Override
            public UserRecord findByCredentials(String username, String password) {
                return new UserRecord(1, "student", "Workshop Student", "STUDENT");
            }
        };
        resource.tokenSecret = Optional.empty();
        LoginRequest request = new LoginRequest();
        request.username = "student";
        request.password = "test-password";
        request.client = "angular-workshop";
        try (var response = resource.login(request)) {
            assertEquals(500, response.getStatus());
            assertFalse(((Map<?, ?>) response.getEntity()).containsKey("token"));
        }
    }

    private void assertLoginError(String requestBody, int statusCode, String message) {
        given()
                .contentType("application/json")
                .body(requestBody)
                .when().post("/auth/login")
                .then()
                .statusCode(statusCode)
                .body("message", is(message));
    }
}
