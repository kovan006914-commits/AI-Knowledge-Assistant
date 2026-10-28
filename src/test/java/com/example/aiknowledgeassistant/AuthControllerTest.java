package com.example.aiknowledgeassistant;

import com.example.aiknowledgeassistant.dto.LoginRequest;
import com.example.aiknowledgeassistant.dto.UserProfile;
import com.example.aiknowledgeassistant.exception.InvalidCredentialsException;
import com.example.aiknowledgeassistant.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AuthControllerTest {
    private UserService service;
    private MockMvc mvc;
    private final UserProfile profile = new UserProfile(1L, "kefan", null);
    private static final String BODY = "{\"username\":\"kefan\",\"password\":\"demo12345\"}";

    @BeforeEach
    void setUp() {
        service = mock(UserService.class);
        mvc = MockMvcBuilders.standaloneSetup(new AuthController(service))
                .setControllerAdvice(new ApiExceptionHandler()).build();
    }

    @Test
    void loginCreatesFreshSessionAndMeReturnsSafeProfile() throws Exception {
        when(service.authenticate(any(LoginRequest.class))).thenReturn(profile);
        MockHttpSession oldSession = new MockHttpSession();
        String oldId = oldSession.getId();
        var result = mvc.perform(post("/user/login").session(oldSession)
                .header("X-Requested-With", "XMLHttpRequest")
                .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isOk()).andExpect(jsonPath("$.user.username").value("kefan"))
                .andExpect(jsonPath("$.user.password").doesNotExist())
                .andReturn();
        assertTrue(oldSession.isInvalid());
        MockHttpSession fresh = (MockHttpSession) result.getRequest().getSession(false);
        assertNotNull(fresh);
        assertNotEquals(oldId, fresh.getId());
        assertEquals(1800, fresh.getMaxInactiveInterval());
        mvc.perform(get("/user/me").session(fresh))
                .andExpect(status().isOk()).andExpect(jsonPath("$.user.id").value(1));
    }

    @Test
    void badCredentialsDoNotCreateSession() throws Exception {
        when(service.authenticate(any(LoginRequest.class)))
                .thenThrow(new InvalidCredentialsException());
        var result = mvc.perform(post("/user/login")
                .header("X-Requested-With", "XMLHttpRequest")
                .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("用户名或密码错误")).andReturn();
        assertNull(result.getRequest().getSession(false));
    }

    @Test
    void meWithoutSessionDoesNotCreateOne() throws Exception {
        var result = mvc.perform(get("/user/me")).andExpect(status().isUnauthorized()).andReturn();
        assertNull(result.getRequest().getSession(false));
    }

    @Test
    void logoutInvalidatesSessionAndIsIdempotent() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(AuthController.CURRENT_USER, profile);
        mvc.perform(post("/user/logout").session(session)
                .header("X-Requested-With", "XMLHttpRequest")).andExpect(status().isOk());
        assertTrue(session.isInvalid());
        var result = mvc.perform(post("/user/logout")
                .header("X-Requested-With", "XMLHttpRequest"))
                .andExpect(status().isOk()).andReturn();
        assertNull(result.getRequest().getSession(false));
        mvc.perform(get("/user/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void missingAjaxHeaderRejectsLoginAndLogout() throws Exception {
        mvc.perform(post("/user/login").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isBadRequest());
        MockHttpSession session = new MockHttpSession();
        mvc.perform(post("/user/logout").session(session)).andExpect(status().isBadRequest());
        assertFalse(session.isInvalid());
        verifyNoInteractions(service);
    }

    @Test
    void malformedJsonReturns400() throws Exception {
        mvc.perform(post("/user/login").header("X-Requested-With", "XMLHttpRequest")
                .contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }
}
