package com.jin.chat.service.impl;

import com.jin.chat.domain.vo.MetricsDashboardVO;
import com.jin.chat.repository.AuditQueueRepository;
import com.jin.chat.repository.LoggedInRepository;
import com.jin.chat.service.MonitorService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.distribution.HistogramSnapshot;
import io.micrometer.core.instrument.distribution.ValueAtPercentile;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * 监控服务实现。基于 Micrometer 采集，汇总供「性能监控」菜单可视化。
 * <ul>
 *     <li>审核处理延迟：Timer + 分位数直方图（P95/P99）</li>
 *     <li>系统错误率：服务端错误计数 / http.server.requests 总请求</li>
 *     <li>数据库连接池：HikariCP gauge（active/idle/max）</li>
 * </ul>
 * </p>
 *
 * @author jinshuai
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MonitorServiceImpl implements MonitorService {

    private final MeterRegistry meterRegistry;
    private final LoggedInRepository loggedInRepository;
    private final AuditQueueRepository auditQueueRepository;

    private Timer auditLatencyTimer;
    private Counter systemErrorCounter;

    @PostConstruct
    public void init() {
        this.auditLatencyTimer = Timer.builder("chat.message.audit.latency")
                .description("消息从提交到审核完成的处理延迟")
                .publishPercentiles(0.95, 0.99)
                .register(meterRegistry);
        this.systemErrorCounter = Counter.builder("chat.system.errors")
                .description("系统级错误（服务端异常）计数")
                .register(meterRegistry);
    }

    @Override
    public void recordAuditLatency(Duration duration) {
        if (duration == null || duration.isNegative()) {
            return;
        }
        auditLatencyTimer.record(duration);
    }

    @Override
    public void recordSystemError() {
        systemErrorCounter.increment();
    }

    @Override
    public MetricsDashboardVO dashboard() {
        MetricsDashboardVO vo = new MetricsDashboardVO();
        vo.setOnlineUsers(loggedInRepository.loggedInCount());
        vo.setAuditQueueLength(auditQueueRepository.pendingSize());

        // 审核延迟分位数
        HistogramSnapshot snapshot = auditLatencyTimer.takeSnapshot();
        vo.setLatencySampleCount(snapshot.count());
        vo.setLatencyAvgMs(round(snapshot.mean(TimeUnit.MILLISECONDS)));
        vo.setLatencyMaxMs(round(snapshot.max(TimeUnit.MILLISECONDS)));
        for (ValueAtPercentile p : snapshot.percentileValues()) {
            double ms = round(p.value(TimeUnit.MILLISECONDS));
            if (Math.abs(p.percentile() - 0.95) < 1e-6) {
                vo.setLatencyP95Ms(ms);
            } else if (Math.abs(p.percentile() - 0.99) < 1e-6) {
                vo.setLatencyP99Ms(ms);
            }
        }

        // 错误率：优先用系统错误计数，其次结合 http.server.requests 总量
        long total = httpRequestCount(null);
        long serverErrors = httpRequestCount("SERVER_ERROR");
        long customErrors = (long) systemErrorCounter.count();
        long errors = Math.max(serverErrors, customErrors);
        vo.setTotalRequests(total);
        vo.setErrorRequests(errors);
        vo.setErrorRate(total > 0 ? round((double) errors / total) : 0d);

        // 数据库连接池（HikariCP）
        double active = gaugeValue("hikaricp.connections.active");
        double idle = gaugeValue("hikaricp.connections.idle");
        double max = gaugeValue("hikaricp.connections.max");
        vo.setDbPoolActive(active);
        vo.setDbPoolIdle(idle);
        vo.setDbPoolMax(max);
        vo.setDbPoolUsage(max > 0 ? round(active / max) : 0d);

        vo.setTimestamp(OffsetDateTime.now());
        return vo;
    }

    /**
     * 统计 http.server.requests 请求数；outcome 为 null 时统计全部。
     */
    private long httpRequestCount(String outcome) {
        long count = 0;
        for (Timer timer : meterRegistry.find("http.server.requests").timers()) {
            if (outcome == null || outcome.equals(timer.getId().getTag("outcome"))) {
                count += timer.count();
            }
        }
        return count;
    }

    private double gaugeValue(String name) {
        double sum = 0;
        boolean found = false;
        for (Gauge gauge : meterRegistry.find(name).gauges()) {
            double v = gauge.value();
            if (!Double.isNaN(v)) {
                sum += v;
                found = true;
            }
        }
        return found ? sum : 0d;
    }

    private double round(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return 0d;
        }
        return Math.round(value * 100d) / 100d;
    }
}
