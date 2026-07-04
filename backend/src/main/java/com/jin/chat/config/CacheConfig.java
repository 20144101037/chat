package com.jin.chat.config;

import com.jin.chat.common.cache.TwoLevelCache;
import com.jin.chat.common.constant.RedisKeyConst;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

/**
 * <p>
 * 缓存相关配置：订阅缓存失效广播频道，收到消息后清除各实例本地 L1，
 * 从而保证多实例部署下二级缓存（Guava + Redis）的一致性。
 * </p>
 *
 * @author jinshuai
 */
@Configuration
public class CacheConfig {

    @Bean
    public RedisMessageListenerContainer cacheInvalidateContainer(RedisConnectionFactory connectionFactory,
                                                                  TwoLevelCache twoLevelCache) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(twoLevelCache, new ChannelTopic(RedisKeyConst.CACHE_INVALIDATE_CHANNEL));
        return container;
    }
}
