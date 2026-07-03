package com.jin.chat.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jin.chat.common.cache.TwoLevelCache;
import com.jin.chat.common.exception.BusinessException;
import com.jin.chat.domain.ao.ConfigUpdateAO;
import com.jin.chat.domain.entity.SysConfigDO;
import com.jin.chat.mapper.SysConfigMapper;
import com.jin.chat.service.impl.SysConfigServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SysConfigServiceTest {

    @Mock
    private SysConfigMapper sysConfigMapper;
    @Mock
    private TwoLevelCache cache;

    @Spy
    @InjectMocks
    private SysConfigServiceImpl sysConfigService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(sysConfigService, "baseMapper", sysConfigMapper);
    }

    @Test
    void update_shouldRejectNonEditable() {
        SysConfigDO config = new SysConfigDO();
        config.setId(1L);
        config.setEditable(false);
        doReturn(config).when(sysConfigService).getById(1L);

        ConfigUpdateAO ao = new ConfigUpdateAO();
        ao.setConfigValue("100");
        assertThrows(BusinessException.class, () -> sysConfigService.update(1L, ao));
    }

    @Test
    void update_shouldValidateNumericRange() {
        SysConfigDO config = new SysConfigDO();
        config.setId(2L);
        config.setEditable(true);
        config.setConfigKey("jwt.expire-minutes");
        doReturn(config).when(sysConfigService).getById(2L);

        ConfigUpdateAO ao = new ConfigUpdateAO();
        ao.setConfigValue("999999");
        assertThrows(BusinessException.class, () -> sysConfigService.update(2L, ao));
    }

    @Test
    void update_shouldPersistValidValue() {
        SysConfigDO config = new SysConfigDO();
        config.setId(3L);
        config.setEditable(true);
        config.setConfigKey("jwt.expire-minutes");
        doReturn(config).when(sysConfigService).getById(3L);
        doReturn(true).when(sysConfigService).updateById(any(SysConfigDO.class));

        ConfigUpdateAO ao = new ConfigUpdateAO();
        ao.setConfigValue("120");
        SysConfigDO updated = sysConfigService.update(3L, ao);

        assertEquals("120", updated.getConfigValue());
        verify(cache).evict(argThat(key -> key.contains("jwt.expire-minutes")));
    }

    @Test
    void getString_shouldUseCacheLoader() {
        when(cache.get(anyString(), any(), any())).thenReturn("cached");
        assertEquals("cached", sysConfigService.getString("jwt.expire-minutes", "60"));
    }

    @Test
    void getInt_shouldReturnDefault_whenUnparseable() {
        when(cache.get(anyString(), any(), any())).thenReturn("abc");
        assertEquals(60, sysConfigService.getInt("jwt.expire-minutes", 60));
    }

    @Test
    void getInt_shouldParseInteger() {
        when(cache.get(anyString(), any(), any())).thenReturn(" 90 ");
        assertEquals(90, sysConfigService.getInt("jwt.expire-minutes", 60));
    }
}
