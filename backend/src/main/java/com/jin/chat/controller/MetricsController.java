package com.jin.chat.controller;

import com.jin.chat.common.api.ResultData;
import com.jin.chat.domain.vo.MetricsDashboardVO;
import com.jin.chat.service.MenuPermissionService;
import com.jin.chat.service.MonitorService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 * 性能监控指标接口：为「性能监控」菜单提供可视化数据。
 * </p>
 *
 * @author jinshuai
 */
@RestController
@RequestMapping("/api/metrics")
@RequiredArgsConstructor
public class MetricsController {

    private static final String MENU_METRICS = "/app/metrics";

    private final MonitorService monitorService;
    private final MenuPermissionService menuPermissionService;

    @GetMapping("/dashboard")
    public ResultData<MetricsDashboardVO> dashboard() {
        menuPermissionService.requireMenuPath(MENU_METRICS);
        return ResultData.success(monitorService.dashboard());
    }
}
