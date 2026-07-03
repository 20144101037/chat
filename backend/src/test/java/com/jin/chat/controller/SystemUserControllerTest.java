package com.jin.chat.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jin.chat.common.api.PageResult;
import com.jin.chat.common.context.LoginUser;
import com.jin.chat.common.context.UserContextHolder;
import com.jin.chat.common.exception.BusinessException;
import com.jin.chat.common.exception.ErrorCodeEnum;
import com.jin.chat.common.exception.GlobalExceptionHandler;
import com.jin.chat.domain.ao.ResetPasswordAO;
import com.jin.chat.domain.vo.AdminUserVO;
import com.jin.chat.service.MenuPermissionService;
import com.jin.chat.service.MonitorService;
import com.jin.chat.service.UserAdminService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SystemUserController.class)
@Import(GlobalExceptionHandler.class)
class SystemUserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserAdminService userAdminService;

    @MockBean
    private MenuPermissionService menuPermissionService;

    @MockBean
    private MonitorService monitorService;

    @BeforeEach
    void setUp() {
        LoginUser user = new LoginUser();
        user.setUserId(1L);
        user.setRole("SYS_ADMIN");
        UserContextHolder.set(user);
        doNothing().when(menuPermissionService).requireMenuPath(anyString());
    }

    @AfterEach
    void tearDown() {
        UserContextHolder.clear();
    }

    @Test
    void page_shouldReturnUsers() throws Exception {
        AdminUserVO vo = new AdminUserVO();
        vo.setId(2L);
        vo.setUsername("lisi");
        PageResult<AdminUserVO> page = PageResult.of(1, 1, 10, List.of(vo));
        when(userAdminService.page(any())).thenReturn(page);

        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.records[0].username").value("lisi"));
    }

    @Test
    void resetPassword_shouldInvokeService() throws Exception {
        ResetPasswordAO ao = new ResetPasswordAO();
        ao.setPassword("newpass1");

        mockMvc.perform(put("/api/admin/users/2/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ao)))
                .andExpect(status().isOk());

        verify(userAdminService).resetPassword(2L, "newpass1");
    }

    @Test
    void resetPassword_shouldValidateLength() throws Exception {
        ResetPasswordAO ao = new ResetPasswordAO();
        ao.setPassword("123");

        mockMvc.perform(put("/api/admin/users/2/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ao)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(ErrorCodeEnum.PARAM_INVALID.getCode()));
    }
}
