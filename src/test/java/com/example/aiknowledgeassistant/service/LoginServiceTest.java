package com.example.aiknowledgeassistant.service;

import com.example.aiknowledgeassistant.dto.LoginRequest;
import com.example.aiknowledgeassistant.dto.UserProfile;
import com.example.aiknowledgeassistant.entity.User;
import com.example.aiknowledgeassistant.exception.InvalidCredentialsException;
import com.example.aiknowledgeassistant.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LoginServiceTest {
    private User account(String hash) {
        User user = new User();
        user.setId(1L);
        user.setUsername("kefan");
        user.setEmail("kefan@example.com");
        user.setPassword(hash);
        return user;
    }

    @Test
    void authenticatesHashAndNormalizesUsernameWithoutWriting() {
        UserMapper mapper = mock(UserMapper.class);
        when(mapper.findByUsername("kefan"))
                .thenReturn(account(new BCryptPasswordEncoder().encode("demo12345")));
        UserProfile profile = new UserService(mapper)
                .authenticate(new LoginRequest("  kefan  ", "demo12345"));
        assertEquals(new UserProfile(1L, "kefan", "kefan@example.com"), profile);
        verify(mapper).findByUsername("kefan");
        verifyNoMoreInteractions(mapper);
    }

    @Test
    void unknownUserAndWrongPasswordHaveSameError() {
        UserMapper mapper = mock(UserMapper.class);
        when(mapper.findByUsername("kefan"))
                .thenReturn(account(new BCryptPasswordEncoder().encode("demo12345")));
        UserService service = new UserService(mapper);
        var missing = assertThrows(InvalidCredentialsException.class,
                () -> service.authenticate(new LoginRequest("missing", "demo12345")));
        var wrong = assertThrows(InvalidCredentialsException.class,
                () -> service.authenticate(new LoginRequest("kefan", "incorrect")));
        assertEquals("用户名或密码错误", missing.getMessage());
        assertEquals(missing.getMessage(), wrong.getMessage());
    }

    @Test
    void rejectsLegacyPlaintextAndDoesNotTrimPassword() {
        UserMapper mapper = mock(UserMapper.class);
        when(mapper.findByUsername("kefan")).thenReturn(account("demo12345"));
        UserService service = new UserService(mapper);
        assertThrows(InvalidCredentialsException.class,
                () -> service.authenticate(new LoginRequest("kefan", "demo12345")));
        when(mapper.findByUsername("kefan"))
                .thenReturn(account(new BCryptPasswordEncoder().encode("demo12345 ")));
        assertThrows(InvalidCredentialsException.class,
                () -> service.authenticate(new LoginRequest("kefan", "demo12345")));
        assertNotNull(service.authenticate(new LoginRequest("kefan", "demo12345 ")));
    }

    @Test
    void invalidInputsNeverQueryDatabase() {
        UserMapper mapper = mock(UserMapper.class);
        UserService service = new UserService(mapper);
        for (LoginRequest request : new LoginRequest[]{
                null, new LoginRequest(null, "demo12345"), new LoginRequest(" ", "demo12345"),
                new LoginRequest("a".repeat(51), "demo12345"),
                new LoginRequest("kefan", null), new LoginRequest("kefan", " "),
                new LoginRequest("kefan", "密".repeat(25))}) {
            assertThrows(IllegalArgumentException.class, () -> service.authenticate(request));
        }
        verifyNoInteractions(mapper);
    }
}
