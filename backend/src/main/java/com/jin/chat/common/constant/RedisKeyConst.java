package com.jin.chat.common.constant;

/**
 * <p>
 * Redis key 前缀常量。
 * </p>
 *
 * @author jinshuai
 */
public final class RedisKeyConst {

    private RedisKeyConst() {
    }

    /** 待审核消息 ID 队列（List，FIFO） */
    public static final String AUDIT_PENDING_QUEUE = "chat:audit:pending:queue";

    /** 待审核消息 ID 有序集合（ZSet，score=提交时间戳，用于超时扫描与按提交顺序处理） */
    public static final String AUDIT_PENDING_ZSET = "chat:audit:pending:zset";

    /** 审核分布式锁前缀： chat:audit:lock:{messageId} */
    public static final String AUDIT_LOCK_PREFIX = "chat:audit:lock:";

    /** 在线用户会话（WebSocket）： chat:session:online:{userId} */
    public static final String SESSION_ONLINE_PREFIX = "chat:session:online:";

    /** 已登录用户（REST 登录成功且未退出/未过期）： chat:auth:logged-in:{userId} */
    public static final String LOGGED_IN_PREFIX = "chat:auth:logged-in:";

    /** 用户已订阅房间集合： chat:session:rooms:{userId} */
    public static final String SESSION_ROOMS_PREFIX = "chat:session:rooms:";

    /** 热门聊天室缓存 */
    public static final String ROOM_HOT_CACHE = "chat:room:hot";

    /** 用户角色 ID 列表缓存前缀： chat:perm:roleIds:{userId} */
    public static final String PERM_ROLE_IDS_PREFIX = "chat:perm:roleIds:";

    /** 用户可见菜单树缓存前缀： chat:perm:menus:{userId} */
    public static final String PERM_MENUS_PREFIX = "chat:perm:menus:";

    /** 全局配置值缓存前缀： chat:config:{configKey} */
    public static final String CONFIG_VALUE_PREFIX = "chat:config:";

    /** 二级缓存失效广播频道（多实例 L1 一致性） */
    public static final String CACHE_INVALIDATE_CHANNEL = "chat:cache:invalidate";

    /** 令牌最小有效签发时间前缀（早于该时间签发的 token 视为失效，用于重置密码/强制下线）： chat:token:minIat:{userId} */
    public static final String TOKEN_MIN_IAT_PREFIX = "chat:token:minIat:";
}
