package com.yanghui.study.kafka.config;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * 配置生产者
 */
@Configuration
public class KafkaProducerConfig {

    @Value("${kafka.servers}")
    private String servers;

    @Bean
    public ProducerFactory<String, String> producerFactory() {
        return new DefaultKafkaProducerFactory<>(producerConfigs());
    }


    /**
     * 生产者配置
     *
     * acks 是判别请求是否为完整的条件（就是是判断是不是成功发送了）。我们指定了“all”将会阻塞消息，这种设置性能最低，但是是最可靠的。
     *
     * retries，如果请求失败，生产者会自动重试，我们指定是0次，如果启用重试，则会有重复消息的可能性。
     *
     * producer(生产者)缓存每个分区未发送的消息。缓存的大小是通过 batch.size 配置指定的。
     * 值较大的话将会产生更大的批。并需要更多的内存（因为每个“活跃”的分区都有1个缓冲区）。
     *
     * 默认缓冲可立即发送，即便缓冲空间还没有满，但是，如果你想减少请求的数量，可以设置linger.ms大于0。
     * 这将指示生产者发送请求之前等待一段时间，希望更多的消息填补到未满的批中。
     * 这类似于TCP的算法，例如上面的代码段，可能100条消息在一个请求发送，因为我们设置了linger(逗留)时间为1毫秒，
     * 然后，如果我们没有填满缓冲区，这个设置将增加1毫秒的延迟请求以等待更多的消息。
     * 需要注意的是，在高负载下，相近的时间一般也会组成批，即使是 linger.ms=0。
     * 在不处于高负载的情况下，如果设置比0大，以少量的延迟代价换取更少的，更有效的请求。
     *
     * buffer.memory 控制生产者可用的缓存总量，如果消息发送速度比其传输到服务器的快，将会耗尽这个缓存空间。
     * 当缓存空间耗尽，其他发送调用将被阻塞，阻塞时间的阈值通过max.block.ms设定，之后它将抛出一个TimeoutException。
     */
    @Bean
    public Map<String, Object> producerConfigs() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, servers);
        props.put("acks", "all");
        props.put("retries", 0);
        props.put("batch.size", 16384);
        props.put("linger.ms", 1);
        props.put("buffer.memory", 33554432);
        props.put("key.serializer", StringSerializer.class);
        props.put("value.serializer", StringSerializer.class);
        return props;
    }
    @Bean
    public KafkaTemplate<String, String> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }
}
