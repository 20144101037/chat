package com.jin.chat.domain.vo;

import lombok.Data;

import java.time.OffsetDateTime;

/**
 * 性能监控仪表盘指标。
 *
 * @author jinshuai
 */
@Data
public class MetricsDashboardVO {

    /** 在线用户数 */
    private long onlineUsers;

    /** 待审核队列长度 */
    private long auditQueueLength;

    /** 消息处理延迟（提交->审核，毫秒） */
    private double latencyP95Ms;
    private double latencyP99Ms;
    private double latencyAvgMs;
    private double latencyMaxMs;
    private long latencySampleCount;

    /** 系统错误率（0~1，服务端 5xx / 总请求） */
    private double errorRate;
    private long totalRequests;
    private long errorRequests;

    /** 数据库连接池 */
    private double dbPoolActive;
    private double dbPoolIdle;
    private double dbPoolMax;
    /** 使用率（0~1，active/max） */
    private double dbPoolUsage;

    /** 采样时间 */
    private OffsetDateTime timestamp;
}
