package com.jin.chat.config;

import com.jin.chat.repository.AuditQueueRepository;
import com.jin.chat.repository.LoggedInRepository;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import jakarta.annotation.PostConstruct;

/**
 * <p>
 * 自定义监控指标注册：已登录用户数、待审核队列长度。
 * 通过 /actuator/prometheus 暴露给 Prometheus 抓取。
 * </p>
 *
 * @author jinshuai
 */
@Configuration
@RequiredArgsConstructor
public class MetricsConfig {

    private final MeterRegistry meterRegistry;
    private final LoggedInRepository loggedInRepository;
    private final AuditQueueRepository auditQueueRepository;

    @PostConstruct
    public void registerMetrics() {
        meterRegistry.gauge("chat.logged-in.users", loggedInRepository,
                LoggedInRepository::loggedInCount);
        meterRegistry.gauge("chat.online.users", loggedInRepository,
                LoggedInRepository::loggedInCount);
        meterRegistry.gauge("chat.audit.pending.size", auditQueueRepository,
                AuditQueueRepository::pendingSize);
    }
}
