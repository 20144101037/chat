package com.jin.chat.service;

import com.jin.chat.domain.vo.MetricsDashboardVO;

import java.time.Duration;

/**
 * <p>
 * 监控服务：采集/记录关键性能指标，并汇总为仪表盘视图。
 * </p>
 *
 * @author jinshuai
 */
public interface MonitorService {

    /**
     * 记录一次消息审核处理延迟（提交 -> 审核完成）。
     */
    void recordAuditLatency(Duration duration);

    /**
     * 记录一次系统错误（服务端异常）。
     */
    void recordSystemError();

    /**
     * 汇总当前监控指标。
     */
    MetricsDashboardVO dashboard();
}
