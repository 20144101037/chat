package com.jin.chat.common.util;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * <p>
 * 事务辅助工具：将副作用（如消息推送）延迟到事务成功提交后执行，
 * 避免事务回滚仍然推送导致的“幽灵消息”，保证关键状态更新的事务性。
 * </p>
 *
 * @author jinshuai
 */
public final class TransactionUtils {

    private TransactionUtils() {
    }

    /**
     * 在当前事务提交后执行；若不在事务中则立即执行。
     */
    public static void afterCommit(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            });
        } else {
            action.run();
        }
    }
}
