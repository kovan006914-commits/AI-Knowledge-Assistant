package com.example.aiknowledgeassistant.service;

import com.example.aiknowledgeassistant.dto.RegisterRequest;
import com.example.aiknowledgeassistant.entity.User;
import com.example.aiknowledgeassistant.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserServiceTest {
    @Test
    void rejectsBlankUsernameWithoutWriting() {
        UserMapper mapper = mock(UserMapper.class);
        UserService service = new UserService(mapper);
        assertThrows(IllegalArgumentException.class,
                () -> service.register(new RegisterRequest("  ", "demo12345", null)));
        verifyNoInteractions(mapper);
    }

    @Test
    void rejectsInvalidPasswordsAndEmailWithoutWriting() {
        UserMapper mapper = mock(UserMapper.class);
        UserService service = new UserService(mapper);
        for (String password : new String[]{null, "", "short", "密".repeat(25)}) {
            assertThrows(IllegalArgumentException.class,
                    () -> service.register(new RegisterRequest("kefan", password, null)));
        }
        assertThrows(IllegalArgumentException.class,
                () -> service.register(new RegisterRequest("kefan", "demo12345", "bad-email")));
        verifyNoInteractions(mapper);
    }

    @Test
    void storesHashAndNormalizesUserFields() {
        UserMapper mapper = mock(UserMapper.class);
        when(mapper.insert(any(User.class))).thenReturn(1);
        new UserService(mapper).register(
                new RegisterRequest("  kefan  ", "demo12345", "  kefan@example.com  "));
        ArgumentCaptor<User> captured = ArgumentCaptor.forClass(User.class);
        verify(mapper).insert(captured.capture());
        User user = captured.getValue();
        assertEquals("kefan", user.getUsername());
        assertEquals("kefan@example.com", user.getEmail());
        assertNotEquals("demo12345", user.getPassword());
        assertTrue(new BCryptPasswordEncoder().matches("demo12345", user.getPassword()));
        assertNull(user.getId());
        assertNull(user.getCreateTime());
    }

    @Test
    void zeroRowsIsNotSuccess() {
        UserMapper mapper = mock(UserMapper.class);
        when(mapper.insert(any(User.class))).thenReturn(0);
        assertThrows(IllegalStateException.class, () -> new UserService(mapper)
                .register(new RegisterRequest("kefan", "demo12345", null)));
    }
}
