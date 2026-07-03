package com.jin.chat.task;

import com.jin.chat.repository.AuditQueueRepository;
import com.jin.chat.service.AuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * <p>
 * 审核超时扫描任务：扫描提交时间超过 maxWaitSeconds 的待审核消息，
 * 自动标记为 TIMEOUT 并通知提交者。超时消息不进入聊天室。
 * </p>
 *
 * <p>注：多实例部署时应结合分布式锁（如 ShedLock）保证同一时刻仅一个实例执行。</p>
 *
 * @author jinshuai
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuditTimeoutTask {

    @Value("${chat.audit.max-wait-seconds}")
    private long maxWaitSeconds;

    private final AuditQueueRepository auditQueueRepository;
    private final AuditService auditService;

    @Scheduled(fixedDelayString = "${chat.audit.scan-interval-ms}")
    public void scanTimeout() {
        long threshold = System.currentTimeMillis() - maxWaitSeconds * 1000;
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
