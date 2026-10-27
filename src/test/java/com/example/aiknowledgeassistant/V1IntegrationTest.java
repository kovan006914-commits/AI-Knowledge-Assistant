package com.example.aiknowledgeassistant;

import java.net.*;
import java.net.http.*;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class V1IntegrationTest {
    @LocalServerPort int port;
    private final JsonMapper json = new JsonMapper();
    private HttpClient a, b, anonymous;
    private String username;
    private long userId;

    private HttpClient client() {
        return HttpClient.newBuilder().cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL)).build();
    }
    private HttpResponse<String> request(HttpClient client, String method, String path, Object body, boolean header)
            throws Exception {
        var builder = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path));
        if (header) builder.header("X-Requested-With", "XMLHttpRequest");
        if (body != null) builder.header("Content-Type", "application/json");
        return client.send(builder.method(method, body == null ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body))).build(),
                HttpResponse.BodyHandlers.ofString());
    }
    private JsonNode data(HttpResponse<String> response) { return json.readTree(response.body()); }
    private Map<String,String> credentials(String name) { return Map.of("username", name, "password", "integration12345"); }
    private Map<String,String> note(String title, String content) {
        return Map.of("title", title, "content", content, "tags", "Java, 学习");
    }
    private long create(HttpClient client, String title, String content) throws Exception {
        var response = request(client, "POST", "/api/knowledge", note(title, content), true);
        assertEquals(201, response.statusCode(), response.body());
        return data(response).path("id").asLong();
    }

    @BeforeEach
    void accounts() throws Exception {
        username = "v1_" + UUID.randomUUID().toString().replace("-", "");
        a = client(); b = client(); anonymous = client();
        assertEquals(201, request(a, "POST", "/user/register", credentials(username), true).statusCode());
        assertEquals(201, request(b, "POST", "/user/register", credentials(username + "b"), true).statusCode());
        var login = request(a, "POST", "/user/login", credentials(username), true);
        assertEquals(200, login.statusCode(), login.body());
        userId = data(login).path("user").path("id").asLong();
        assertEquals(200, request(b, "POST", "/user/login", credentials(username + "b"), true).statusCode());
    }

    @Test
    void registrationValidationAndDuplicates() throws Exception {
        assertEquals(409, request(a, "POST", "/user/register", credentials(username), true).statusCode());
        assertEquals(400, request(a, "POST", "/user/register",
                Map.of("username", " ", "password", "integration12345"), true).statusCode());
        assertEquals(400, request(a, "POST", "/user/register",
                Map.of("username", "new", "password", "short"), true).statusCode());
    }

    @Test
    void loginCookieAndLogoutWorkOverHttp() throws Exception {
        var login = request(a, "POST", "/user/login", credentials(username), true);
        String cookie = login.headers().firstValue("set-cookie").orElseThrow();
        assertTrue(cookie.contains("HttpOnly"), cookie);
        assertTrue(cookie.contains("SameSite=Lax"), cookie);
        assertFalse(login.body().contains("password"));
        var me = request(a, "GET", "/user/me", null, true);
        assertEquals(200, me.statusCode());
        assertEquals(username, data(me).path("user").path("username").asText());
        assertEquals(200, request(a, "POST", "/user/logout", null, true).statusCode());
        assertEquals(401, request(a, "GET", "/user/me", null, true).statusCode());
        assertEquals(401, request(a, "GET", "/api/knowledge", null, true).statusCode());
    }

    @Test
    void invalidLoginAndMissingHeadersAreRejected() throws Exception {
        assertEquals(401, request(anonymous, "POST", "/user/login",
                Map.of("username", username, "password", "wrongpassword"), true).statusCode());
        assertEquals(400, request(anonymous, "POST", "/user/login", credentials(username), false).statusCode());
        assertEquals(403, request(a, "POST", "/api/knowledge", note("test", "content"), false).statusCode());
    }

    @Test
    void completeKnowledgeCrud() throws Exception {
        long id = create(a, "Spring Boot 笔记", "Controller 接收HTTP请求");
        var item = request(a, "GET", "/api/knowledge/" + id, null, true);
        assertEquals(userId, data(item).path("ownerId").asLong());
        assertEquals("Spring Boot 笔记", data(item).path("title").asText());
        var changed = request(a, "PUT", "/api/knowledge/" + id, note("更新标题", "更新正文"), true);
        assertEquals(200, changed.statusCode(), changed.body());
        assertEquals("更新正文", data(changed).path("content").asText());
        assertEquals(200, request(a, "DELETE", "/api/knowledge/" + id, null, true).statusCode());
        assertEquals(404, request(a, "GET", "/api/knowledge/" + id, null, true).statusCode());
    }

    @Test
    void usersCannotReadUpdateDeleteOrSearchEachOthersNotes() throws Exception {
        long id = create(a, "private secret", "owner-only content");
        assertEquals(404, request(b, "GET", "/api/knowledge/" + id, null, true).statusCode());
        assertEquals(404, request(b, "PUT", "/api/knowledge/" + id, note("attack", "overwrite"), true).statusCode());
        assertEquals(404, request(b, "DELETE", "/api/knowledge/" + id, null, true).statusCode());
        var list = request(b, "GET", "/api/knowledge?q=private", null, true);
        assertEquals(0, data(list).path("total").asInt());
        var answer = request(b, "POST", "/api/ask", Map.of("question", "private secret"), true);
        assertEquals(0, data(answer).path("sources").size());
        assertFalse(answer.body().contains("owner-only content"));
        assertEquals(200, request(a, "GET", "/api/knowledge/" + id, null, true).statusCode());
    }

    @Test
    void searchUsesLiteralWildcardsAndPaging() throws Exception {
        create(a, "100% 正确", "first");
        create(a, "other", "second");
        var escaped = request(a, "GET", "/api/knowledge?q=%25", null, true);
        assertEquals(1, data(escaped).path("total").asInt(), escaped.body());
        var paged = request(a, "GET", "/api/knowledge?page=1&size=1", null, true);
        assertEquals(2, data(paged).path("total").asInt());
        assertEquals(1, data(paged).path("items").size());
        assertEquals(400, request(a, "GET", "/api/knowledge?page=0", null, true).statusCode());
    }

    @Test
    void inputLimitsAndAuthenticationAreEnforced() throws Exception {
        assertEquals(401, request(anonymous, "GET", "/api/knowledge", null, true).statusCode());
        assertEquals(401, request(anonymous, "POST", "/api/ask", Map.of("question", "test"), true).statusCode());
        assertEquals(400, request(a, "POST", "/api/knowledge", note(" ", "content"), true).statusCode());
        assertEquals(400, request(a, "POST", "/api/knowledge", note("a".repeat(121), "content"), true).statusCode());
        assertEquals(400, request(a, "POST", "/api/knowledge", note("title", "a".repeat(20001)), true).statusCode());
        assertEquals(400, request(a, "POST", "/api/ask", Map.of("question", ""), true).statusCode());
    }

    @Test
    void questionsReturnLocalSourcesAndNeverInventAiMode() throws Exception {
        long id = create(a, "Spring Boot Controller", "Controller 接收HTTP请求，Service处理业务逻辑。");
        var answer = request(a, "POST", "/api/ask", Map.of("question", "Controller 有什么作用"), true);
        assertEquals(200, answer.statusCode(), answer.body());
        assertEquals("retrieval", data(answer).path("mode").asText());
        assertEquals(id, data(answer).path("sources").path(0).path("id").asLong());
        assertTrue(data(answer).path("answer").asText().contains("Controller"));
    }

    @Test
    void questionRateLimitIsPerUser() throws Exception {
        for (int i=0; i<10; i++)
            assertEquals(200, request(a, "POST", "/api/ask", Map.of("question", "nothing"), true).statusCode());
        assertEquals(429, request(a, "POST", "/api/ask", Map.of("question", "nothing"), true).statusCode());
        assertEquals(200, request(b, "POST", "/api/ask", Map.of("question", "nothing"), true).statusCode());
    }

    @Test
    void servesFrontendWithSecurityHeaders() throws Exception {
        var response = request(anonymous, "GET", "/", null, false);
        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("我的知识库"));
        assertTrue(response.headers().firstValue("content-security-policy").orElse("").contains("frame-ancestors 'none'"));
        assertEquals(200, request(anonymous, "GET", "/app.js", null, false).statusCode());
    }
}
