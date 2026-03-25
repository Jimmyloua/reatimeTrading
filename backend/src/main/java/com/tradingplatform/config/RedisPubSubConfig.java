package com.tradingplatform.config;

import com.tradingplatform.chat.redis.RedisChannels;
import com.tradingplatform.chat.redis.RedisChatEventSubscriber;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

@Configuration
public class RedisPubSubConfig {

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
        RedisConnectionFactory redisConnectionFactory,
        RedisChatEventSubscriber redisChatEventSubscriber
    ) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(redisConnectionFactory);
        container.addMessageListener(redisChatEventSubscriber, new ChannelTopic(RedisChannels.MESSAGE_CHANNEL));
        container.addMessageListener(redisChatEventSubscriber, new ChannelTopic(RedisChannels.PRESENCE_CHANNEL));
        return container;
    }
}
