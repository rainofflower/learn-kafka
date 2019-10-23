package com.yanghui.study.kafka.config;

import org.apache.kafka.common.serialization.LongDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * 配置消费者
 */
@Configuration
public class KafkaConsumerConfig {

    @Value("${kafka.servers}")
    private String servers;

    /**
     * 消费者归属于一个消费者群组里
     * 若不配置群组，kafka会将该消费者划到默认的群组中
     * 在同一个群组中，consumer和partition是一对多关系，一个partition内的消息只能被同一个组中的一个consumer消费
     * 不同群组中的消费者互不影响，可重复消费一个topic里的消息
     */
    @Value("${kafka.groupId1}")
    private String groupId;

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory(){
        ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        factory.setBatchListener(true);
        factory.setConcurrency(3);
        return factory;
    }

    @Bean
    public ConsumerFactory<String,String> consumerFactory(){
        return new DefaultKafkaConsumerFactory<>(consumerConfigs());
    }

    @Bean
    public Map<String,Object> consumerConfigs(){
        HashMap<String, Object> props = new HashMap<>();
        props.put("bootstrap.servers", servers);
        props.put("group.id", groupId);
        props.put("enable.auto.commit", "true");
        props.put("auto.commit.interval.ms", "1000");
        props.put("key.deserializer", StringDeserializer.class);
        props.put("value.deserializer", StringDeserializer.class); //LongDeserializer.class
        return props;
    }
}
