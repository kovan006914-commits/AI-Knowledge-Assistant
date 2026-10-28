package com.example.aiknowledgeassistant.service;

import com.example.aiknowledgeassistant.entity.KnowledgeItem;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AskServiceTest {
    @Test
    void providerFailureFallsBackToExplicitRetrieval() {
        KnowledgeService knowledge = mock(KnowledgeService.class);
        AiGateway gateway = mock(AiGateway.class);
        KnowledgeItem item = new KnowledgeItem();
        item.setId(1L); item.setTitle("Spring Boot"); item.setContent("Spring Boot notes"); item.setTags("");
        when(knowledge.recentForSearch(7)).thenReturn(List.of(item));
        when(gateway.enabled()).thenReturn(true);
        when(gateway.answer(anyString(), anyString())).thenThrow(new IllegalStateException("failure"));
        var answer = new AskService(knowledge, gateway).ask(7, "Spring Boot");
        assertEquals("retrieval", answer.mode());
        assertNotNull(answer.warning());
        assertEquals(1, answer.sources().size());
        verify(knowledge).recentForSearch(7);
    }

    @Test
    void noRelevantSourceNeverCallsProvider() {
        KnowledgeService knowledge = mock(KnowledgeService.class);
        AiGateway gateway = mock(AiGateway.class);
        when(knowledge.recentForSearch(7)).thenReturn(List.of());
        var answer = new AskService(knowledge, gateway).ask(7, "what is missing");
        assertEquals("retrieval", answer.mode());
        assertTrue(answer.sources().isEmpty());
        verifyNoInteractions(gateway);
    }
}
