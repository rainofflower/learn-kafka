package com.yanghui.study.kafka.config;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaAdmin;

/**
 * 创建topic
 */
//@Configuration //可以不提前创建，直接生产消息(kafka会自动创建,只要在配置文件里配置了auto.create.topics.enable=true)
public class KafkaTopicConfig {

    @Value("${kafka.servers}")
    private String servers;

    @Value("${kafka.topic1}")
    private String topic;

    /**
     * kafkaAdmin用于创建topic
     * @return
     */
    @Bean
    public KafkaAdmin admin() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, servers);
        return new KafkaAdmin(configs);
    }

    /**
     * Topic配置,如果不配置topic直接生产消息kafka将会自动创建topic
     */
    @Bean
    public NewTopic topic1() {
        return new NewTopic(topic, 3, (short) 2);
    }

}
