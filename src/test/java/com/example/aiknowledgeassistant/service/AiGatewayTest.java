package com.example.aiknowledgeassistant.service;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AiGatewayTest {
    @Test
    void sendsGroundedContextAndParsesCompatibleResponse() throws Exception {
        var seen = new AtomicReference<String>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/chat/completions", exchange -> {
            seen.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] body = "{\"choices\":[{\"message\":{\"content\":\"根据资料[1]：Controller接收请求。\"}}]}"
                    .getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type","application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body); exchange.close();
        });
        server.start();
        try {
            AiGateway gateway = new AiGateway("test-only-key", "http://127.0.0.1:" + server.getAddress().getPort()+"/v1", "test-model");
            assertEquals("根据资料[1]：Controller接收请求。", gateway.answer("Controller做什么", "[1] Controller 接收请求"));
            assertTrue(seen.get().contains("test-model"));
            assertTrue(seen.get().contains("Controller"));
        } finally { server.stop(0); }
    }

    @Test
    void providerFailureDoesNotLeakProviderBody() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/chat/completions", exchange -> {
            byte[] body = "private-provider-error".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(503, body.length);
            exchange.getResponseBody().write(body); exchange.close();
        });
        server.start();
        try {
            AiGateway gateway = new AiGateway("test-only-key", "http://127.0.0.1:" + server.getAddress().getPort()+"/v1", "test-model");
            var error = assertThrows(IllegalStateException.class, () -> gateway.answer("question","context"));
            assertFalse(error.getMessage().contains("private-provider-error"));
        } finally { server.stop(0); }
    }
}
