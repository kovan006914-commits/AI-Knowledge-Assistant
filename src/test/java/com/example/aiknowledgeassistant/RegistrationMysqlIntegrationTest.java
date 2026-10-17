package com.example.aiknowledgeassistant;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfEnvironmentVariable(named = "RUN_MYSQL_INTEGRATION", matches = "true")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RegistrationMysqlIntegrationTest {
    @LocalServerPort
    private int port;
    @Autowired
    private JdbcTemplate jdbc;
    private String username;
    private final HttpClient client = HttpClient.newHttpClient();

    @BeforeEach
    void uniqueTestUser() {
        username = "it_" + UUID.randomUUID().toString().replace("-", "");
    }

    @AfterEach
    void removeOnlyThisTestUser() {
        jdbc.update("DELETE FROM user WHERE username = ?", username);
        jdbc.update("DELETE FROM user WHERE username = ?", username + "_2");
        assertEquals(0, count());
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM user WHERE username = ?", Integer.class, username + "_2"));
    }

    private int count() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM user WHERE username = ?", Integer.class, username);
    }

    private HttpResponse<String> post(String json) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/user/register"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json)).build(), HttpResponse.BodyHandlers.ofString());
    }

    private String validRequest() {
        return "{\"username\":\"  " + username + "  \",\"password\":\"integration12345\",\"email\":\""
                + username + "@example.com\",\"id\":999999999,\"createTime\":\"2000-01-01T00:00:00\"}";
    }

    @Test
    void registersAndStoresBcryptWithoutClientControlledId() throws Exception {
        HttpResponse<String> response = post(validRequest());
        assertEquals(201, response.statusCode());
        assertTrue(response.headers().firstValue("content-type").orElse("").contains("application/json"));
        assertTrue(response.body().contains("\"success\":true"));
        assertFalse(response.body().contains("integration12345"));
        String hash = jdbc.queryForObject("SELECT password FROM user WHERE username = ?", String.class, username);
        assertTrue(new BCryptPasswordEncoder().matches("integration12345", hash));
        assertNotEquals(999999999L, jdbc.queryForObject("SELECT id FROM user WHERE username = ?", Long.class, username));
        assertEquals(1, count());
    }

    @Test
    void duplicateUsernameReturnsConflictWithoutExtraRow() throws Exception {
        assertEquals(201, post(validRequest()).statusCode());
        HttpResponse<String> duplicate = post(validRequest());
        assertEquals(409, duplicate.statusCode());
        assertTrue(duplicate.body().contains("\"success\":false"));
        assertEquals(1, count());
    }

    @Test
    void invalidPasswordDoesNotWrite() throws Exception {
        HttpResponse<String> response = post("{\"username\":\"" + username + "\",\"password\":\"short\"}");
        assertEquals(400, response.statusCode());
        assertTrue(response.body().contains("\"success\":false"));
        assertEquals(0, count());
    }

    @Test
    void malformedJsonDoesNotWrite() throws Exception {
        HttpResponse<String> response = post("{");
        assertEquals(400, response.statusCode());
        assertTrue(response.body().contains("\"success\":false"));
        assertEquals(0, count());
    }

    @Test
    void duplicateEmailReturnsConflictForDifferentUsername() throws Exception {
        assertEquals(201, post(validRequest()).statusCode());
        String other = "{\"username\":\"" + username + "_2\",\"password\":\"integration12345\",\"email\":\""
                + username + "@example.com\"}";
        assertEquals(409, post(other).statusCode());
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM user WHERE username = ?", Integer.class, username + "_2"));
    }

    @Test
    void accepts254CharacterEmailAndRejects255() throws Exception {
        String email = username + "a".repeat(64 - username.length()) + "@"
                + "b".repeat(63) + "." + "c".repeat(63) + "." + "d".repeat(61);
        assertEquals(254, email.length());
        String request = "{\"username\":\"" + username + "\",\"password\":\"integration12345\",\"email\":\"";
        assertEquals(400, post(request + email + "x\"}").statusCode());
        assertEquals(0, count());
        assertEquals(201, post(request + email + "\"}").statusCode());
        assertEquals(email, jdbc.queryForObject("SELECT email FROM user WHERE username = ?", String.class, username));
    }

    @Test
    void multipleUsersCanOmitEmail() throws Exception {
        String request = "{\"username\":\"%s\",\"password\":\"integration12345\"}";
        assertEquals(201, post(request.formatted(username)).statusCode());
        assertEquals(201, post(request.formatted(username + "_2")).statusCode());
        assertNull(jdbc.queryForObject("SELECT email FROM user WHERE username = ?", String.class, username));
        assertNull(jdbc.queryForObject("SELECT email FROM user WHERE username = ?", String.class, username + "_2"));
    }
}
