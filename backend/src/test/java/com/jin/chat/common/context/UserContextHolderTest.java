package com.jin.chat.common.context;

import com.jin.chat.common.exception.BusinessException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserContextHolderTest {

    @AfterEach
    void tearDown() {
        UserContextHolder.clear();
    }

    @Test
    void require_shouldThrow_whenNotLoggedIn() {
        assertThrows(BusinessException.class, UserContextHolder::require);
    }

    @Test
    void currentUserId_shouldReturnId() {
        LoginUser user = new LoginUser();
        user.setUserId(88L);
        UserContextHolder.set(user);
        assertEquals(88L, UserContextHolder.currentUserId());
        assertSame(user, UserContextHolder.get());
    }

    @Test
    void clear_shouldRemoveContext() {
        LoginUser user = new LoginUser();
        user.setUserId(1L);
        UserContextHolder.set(user);
        UserContextHolder.clear();
        assertNull(UserContextHolder.get());
    }
}
