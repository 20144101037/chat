package com.jin.chat.common.context;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LoginUserTest {

    @Test
    void isAdmin_shouldBeTrue_forRoomAdmin() {
        LoginUser user = new LoginUser();
        user.setRole("ROOM_ADMIN");
        assertTrue(user.isAdmin());
        assertFalse(user.isSysAdmin());
    }

    @Test
    void isAdmin_shouldBeTrue_forSysAdmin() {
        LoginUser user = new LoginUser();
        user.setRole("SYS_ADMIN");
        assertTrue(user.isAdmin());
        assertTrue(user.isSysAdmin());
    }

    @Test
    void isAdmin_shouldBeFalse_forUser() {
        LoginUser user = new LoginUser();
        user.setRole("USER");
        assertFalse(user.isAdmin());
        assertFalse(user.isSysAdmin());
    }

    @Test
    void isAdmin_shouldBeFalse_whenRoleNull() {
        LoginUser user = new LoginUser();
        assertFalse(user.isAdmin());
        assertFalse(user.isSysAdmin());
    }
}
