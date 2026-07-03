package com.jin.chat.common.api;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PageResultTest {

    @Test
    void of_shouldHandleNullRecords() {
        PageResult<String> page = PageResult.of(0, 1, 10, null);
        assertNotNull(page.getRecords());
        assertTrue(page.getRecords().isEmpty());
    }

    @Test
    void of_shouldSetFields() {
        PageResult<Integer> page = PageResult.of(2, 1, 10, List.of(1, 2));
        assertEquals(2, page.getTotal());
        assertEquals(1, page.getPageNo());
        assertEquals(10, page.getPageSize());
        assertEquals(2, page.getRecords().size());
    }
}
