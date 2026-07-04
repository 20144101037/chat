package com.jin.chat.controller;

import com.jin.chat.common.api.PageResult;
import com.jin.chat.common.api.ResultData;
import com.jin.chat.domain.ao.ConfigUpdateAO;
import com.jin.chat.domain.entity.SysConfigDO;
import com.jin.chat.domain.query.ConfigQuery;
import com.jin.chat.domain.vo.ConfigRuntimeVO;
import com.jin.chat.service.MenuPermissionService;
import com.jin.chat.service.SysConfigService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 * 全局配置管理接口：分页查询、修改配置值。
 * 配置项由系统预置，不支持新增与删除，仅允许修改其值。
 * </p>
 *
 * @author jinshuai
 */
@RestController
@RequestMapping("/api/configs")
@RequiredArgsConstructor
public class SysConfigController {

    private static final String MENU_CONFIGS = "/app/configs";

    private final SysConfigService sysConfigService;
    private final MenuPermissionService menuPermissionService;

    @GetMapping
    public ResultData<PageResult<SysConfigDO>> page(ConfigQuery query) {
        menuPermissionService.requireMenuPath(MENU_CONFIGS);
        return ResultData.success(sysConfigService.page(query));
    }

    /** 运行时配置快照（登录即可读，供创建聊天室/发消息等使用） */
    @GetMapping("/runtime")
    public ResultData<ConfigRuntimeVO> runtime() {
        return ResultData.success(sysConfigService.getRuntime());
    }

    @PutMapping("/{id}")
    public ResultData<SysConfigDO> update(@PathVariable Long id, @Valid @RequestBody ConfigUpdateAO ao) {
        menuPermissionService.requireMenuPath(MENU_CONFIGS);
        return ResultData.success(sysConfigService.update(id, ao));
    }
}
