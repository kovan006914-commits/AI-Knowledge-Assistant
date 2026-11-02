package com.example.aiknowledgeassistant;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.io.ByteArrayOutputStream;
import java.awt.image.BufferedImage;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
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
class KnowledgeImportHttpTest {
    @LocalServerPort int port;
    private final JsonMapper json = new JsonMapper();
    private HttpClient owner, other, anonymous;

    private HttpClient client() {
        return HttpClient.newBuilder().cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL)).build();
    }
    private URI uri(String path) { return URI.create("http://localhost:" + port + path); }
    private JsonNode data(HttpResponse<String> response) { return json.readTree(response.body()); }
    private HttpResponse<String> jsonRequest(HttpClient client, String method, String path, Object body) throws Exception {
        var builder = HttpRequest.newBuilder(uri(path)).header("X-Requested-With", "XMLHttpRequest");
        if (body != null) builder.header("Content-Type", "application/json");
        return client.send(builder.method(method, body == null ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body))).build(),
                HttpResponse.BodyHandlers.ofString());
    }
    private HttpResponse<String> upload(HttpClient client, String filename, byte[] bytes, boolean header) throws Exception {
        String boundary = "Boundary" + UUID.randomUUID().toString().replace("-", "");
        String prefix = "--" + boundary + "\r\nContent-Disposition: form-data; name=\"file\"; filename=\""
                + filename + "\"\r\nContent-Type: application/octet-stream\r\n\r\n";
        String suffix = "\r\n--" + boundary + "--\r\n";
        var builder = HttpRequest.newBuilder(uri("/api/knowledge/import"))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary);
        if (header) builder.header("X-Requested-With", "XMLHttpRequest");
        return client.send(builder.POST(HttpRequest.BodyPublishers.concat(
                HttpRequest.BodyPublishers.ofString(prefix), HttpRequest.BodyPublishers.ofByteArray(bytes),
                HttpRequest.BodyPublishers.ofString(suffix))).build(), HttpResponse.BodyHandlers.ofString());
    }
    private HttpResponse<String> upload(HttpClient client, String filename, String text) throws Exception {
        return upload(client, filename, text.getBytes(StandardCharsets.UTF_8), true);
    }
    @BeforeEach
    void accounts() throws Exception {
        owner = client(); other = client(); anonymous = client();
        for (HttpClient client : new HttpClient[]{owner, other}) {
            var credentials = Map.of("username", "imp_" + UUID.randomUUID().toString().replace("-", ""),
                    "password", "IntegrationTest12345");
            assertEquals(201, jsonRequest(client, "POST", "/user/register", credentials).statusCode());
            assertEquals(200, jsonRequest(client, "POST", "/user/login", credentials).statusCode());
        }
    }

    @Test
    void markdownImportSearchQuestionAndOwnership() throws Exception {
        var imported = upload(owner, "spring.md",
                "# Controller\n\nController 接收 HTTP 请求。\n\n# Service\n\nService 处理业务逻辑。\n");
        assertEquals(201, imported.statusCode(), imported.body());
        assertEquals(2, data(imported).path("count").asInt());
        var search = jsonRequest(owner, "GET", "/api/knowledge?q=Controller", null);
        assertEquals(1, data(search).path("total").asInt(), search.body());
        JsonNode item = data(search).path("items").path(0);
        assertEquals("spring.md - Controller", item.path("title").asText());
        assertEquals("spring.md", item.path("tags").asText());
        var answer = jsonRequest(owner, "POST", "/api/ask", Map.of("question", "Controller"));
        assertEquals(200, answer.statusCode(), answer.body());
        assertEquals(item.path("id").asLong(), data(answer).path("sources").path(0).path("id").asLong());
        assertTrue(data(answer).path("answer").asText().contains("Controller"));
        assertEquals(0, data(jsonRequest(other, "GET", "/api/knowledge?q=Controller", null)).path("total").asInt());
        assertEquals(404, jsonRequest(other, "GET", "/api/knowledge/" + item.path("id").asLong(), null).statusCode());
    }

    @Test
    void textBlankLinesAndLongSectionsAreSplit() throws Exception {
        var imported = upload(owner, "notes.txt", "第一段\n\n" + "x".repeat(501));
        assertEquals(201, imported.statusCode(), imported.body());
        assertEquals(3, data(imported).path("count").asInt(), imported.body());
        var listed = jsonRequest(owner, "GET", "/api/knowledge?q=notes.txt", null);
        assertEquals(3, data(listed).path("total").asInt(), listed.body());
    }

    @Test
    void authenticationHeaderAndInvalidUploadsAreRejected() throws Exception {
        assertEquals(401, upload(anonymous, "notes.txt", "content").statusCode());
        assertEquals(403, upload(owner, "notes.txt", "content".getBytes(StandardCharsets.UTF_8), false).statusCode());
        var wrongType = upload(owner, "program.exe", "not a text document");
        assertEquals(400, wrongType.statusCode(), wrongType.body());
        assertTrue(data(wrongType).path("message").asText().contains(".txt"));
        assertEquals(400, upload(owner, "empty.txt", "").statusCode());
        var invalid = upload(owner, "invalid.txt", new byte[]{(byte)0xff}, true);
        assertEquals(400, invalid.statusCode(), invalid.body());
        assertTrue(data(invalid).path("message").asText().contains("UTF-8"));
        assertEquals(0, data(jsonRequest(owner, "GET", "/api/knowledge", null)).path("total").asInt());
    }

    @Test
    void fileOverFiveMegabytesIsRejectedClearly() throws Exception {
        byte[] bytes = new byte[5 * 1024 * 1024 + 1];
        java.util.Arrays.fill(bytes, (byte)'a');
        var response = upload(owner, "large.txt", bytes, true);
        assertEquals(413, response.statusCode(), response.body());
        assertTrue(data(response).path("message").asText().contains("5MB"), response.body());
    }

    private byte[] pdf(boolean imageOnly, boolean encrypted) throws Exception {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
                if (imageOnly) {
                    BufferedImage image = new BufferedImage(80, 40, BufferedImage.TYPE_INT_RGB);
                    stream.drawImage(LosslessFactory.createFromImage(document, image), 60, 700);
                } else {
                    stream.beginText();
                    stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                    stream.newLineAtOffset(60, 700);
                    stream.showText("Controller receives requests for PDF testing.");
                    stream.endText();
                }
            }
            if (encrypted) {
                var policy = new StandardProtectionPolicy("owner-secret", "user-secret", new AccessPermission());
                policy.setEncryptionKeyLength(128);
                document.protect(policy);
            }
            document.save(output);
            return output.toByteArray();
        }
    }

    @Test
    void pdfTextCanBeImportedSearchedAndUsedForQuestions() throws Exception {
        var response = upload(owner, "lesson.pdf", pdf(false, false), true);
        assertEquals(201, response.statusCode(), response.body());
        assertEquals(1, data(response).path("count").asInt());
        var list = jsonRequest(owner, "GET", "/api/knowledge?q=Controller", null);
        assertEquals(1, data(list).path("total").asInt(), list.body());
        assertEquals("lesson.pdf", data(list).path("items").path(0).path("title").asText());
        var answer = jsonRequest(owner, "POST", "/api/ask", Map.of("question", "Controller"));
        assertEquals(200, answer.statusCode(), answer.body());
        assertTrue(data(answer).path("answer").asText().contains("Controller"), answer.body());
        assertEquals(0, data(jsonRequest(other, "GET", "/api/knowledge?q=Controller", null)).path("total").asInt());
    }

    @Test
    void scannedCorruptedAndEncryptedPdfReturnClearErrors() throws Exception {
        var scanned = upload(owner, "scan.pdf", pdf(true, false), true);
        assertEquals(400, scanned.statusCode(), scanned.body());
        assertTrue(data(scanned).path("message").asText().contains("扫描件"), scanned.body());
        var corrupted = upload(owner, "broken.pdf", "not a pdf".getBytes(StandardCharsets.UTF_8), true);
        assertEquals(400, corrupted.statusCode(), corrupted.body());
        assertTrue(data(corrupted).path("message").asText().contains("PDF 读取失败"), corrupted.body());
        var encrypted = upload(owner, "locked.pdf", pdf(false, true), true);
        assertEquals(400, encrypted.statusCode(), encrypted.body());
        assertTrue(data(encrypted).path("message").asText().contains("加密"), encrypted.body());
        assertEquals(0, data(jsonRequest(owner, "GET", "/api/knowledge", null)).path("total").asInt());
    }
}
