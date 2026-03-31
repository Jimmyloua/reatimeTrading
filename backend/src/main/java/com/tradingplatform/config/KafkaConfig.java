package com.tradingplatform.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tradingplatform.chat.kafka.ChatDeliveryEvent;
import com.tradingplatform.chat.outbox.ChatOutboxRelay;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.LongDeserializer;
import org.apache.kafka.common.serialization.LongSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
@RequiredArgsConstructor
public class KafkaConfig {

    private final ObjectMapper objectMapper;

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id}")
    private String chatDeliveryGroupId;

    @Value("${chat.delivery.topic.partitions:1}")
    private int topicPartitions;

    @Value("${chat.delivery.topic.replication-factor:1}")
    private short topicReplicationFactor;

    @Bean
    public ProducerFactory<Long, ChatDeliveryEvent> chatDeliveryProducerFactory() {
        Map<String, Object> properties = new HashMap<>();
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, LongSerializer.class);
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        properties.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, false);
        return new DefaultKafkaProducerFactory<>(
            properties,
            new LongSerializer(),
            new JsonSerializer<>(objectMapper.copy())
        );
    }

    @Bean
    public KafkaTemplate<Long, ChatDeliveryEvent> chatDeliveryKafkaTemplate() {
        return new KafkaTemplate<>(chatDeliveryProducerFactory());
    }

    @Bean
    public ConsumerFactory<Long, ChatDeliveryEvent> chatDeliveryConsumerFactory() {
        JsonDeserializer<ChatDeliveryEvent> valueDeserializer =
            new JsonDeserializer<>(ChatDeliveryEvent.class, objectMapper.copy(), false);
        valueDeserializer.addTrustedPackages("com.tradingplatform.chat.kafka", "com.tradingplatform.chat.entity");

        Map<String, Object> properties = new HashMap<>();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, chatDeliveryGroupId);
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, LongDeserializer.class);
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        properties.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        properties.put(JsonDeserializer.VALUE_DEFAULT_TYPE, ChatDeliveryEvent.class.getName());
        properties.put(JsonDeserializer.TRUSTED_PACKAGES, "com.tradingplatform.chat.kafka,com.tradingplatform.chat.entity");

        return new DefaultKafkaConsumerFactory<>(
            properties,
            new LongDeserializer(),
            valueDeserializer
        );
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<Long, ChatDeliveryEvent> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<Long, ChatDeliveryEvent> factory =
            new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(chatDeliveryConsumerFactory());
        return factory;
    }

    @Bean
    public KafkaAdmin.NewTopics chatDeliveryTopic() {
        return new KafkaAdmin.NewTopics(
            new NewTopic(ChatOutboxRelay.TOPIC, topicPartitions, topicReplicationFactor)
        );
    }
}
