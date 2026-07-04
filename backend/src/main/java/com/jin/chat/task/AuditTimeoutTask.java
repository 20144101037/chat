package com.jin.chat.task;

import com.jin.chat.common.constant.SysConfigKeys;
import com.jin.chat.repository.AuditQueueRepository;
import com.jin.chat.service.AuditService;
import com.jin.chat.service.SysConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * <p>
 * 审核超时扫描任务：扫描提交时间超过 maxWaitSeconds 的待审核消息，
 * 自动标记为 TIMEOUT 并通知提交者。超时阈值与扫描间隔均从全局配置读取。
 * </p>
 *
 * <p>注：多实例部署时应结合分布式锁（如 ShedLock）保证同一时刻仅一个实例执行。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuditTimeoutTask {

    private final AuditQueueRepository auditQueueRepository;
    private final AuditService auditService;
    private final SysConfigService sysConfigService;

    private volatile long lastScanMs = 0;

    /** 每秒检查一次是否到达配置的扫描间隔 */
    @Scheduled(fixedDelay = 1000)
    public void scanTimeout() {
        long intervalMs = sysConfigService.getInt(SysConfigKeys.AUDIT_SCAN_INTERVAL_MS,
                SysConfigKeys.DEFAULT_AUDIT_SCAN_INTERVAL_MS);
        long now = System.currentTimeMillis();
        if (now - lastScanMs < intervalMs) {
            return;
        }
        lastScanMs = now;

        long maxWaitSeconds = sysConfigService.getInt(SysConfigKeys.AUDIT_MAX_WAIT_SECONDS,
                SysConfigKeys.DEFAULT_AUDIT_MAX_WAIT_SECONDS);
        long threshold = now - maxWaitSeconds * 1000;
        Set<Object> timeoutIds = auditQueueRepository.scanTimeout(threshold);
        if (timeoutIds == null || timeoutIds.isEmpty()) {
            return;
        }
        log.info("扫描到 {} 条超时待审核消息，开始处理", timeoutIds.size());
        for (Object idObj : timeoutIds) {
            try {
                Long messageId = Long.valueOf(String.valueOf(idObj));
                auditService.handleTimeout(messageId);
            } catch (Exception e) {
                log.error("处理超时消息失败 id={}", idObj, e);
            }
        }
    }
}
