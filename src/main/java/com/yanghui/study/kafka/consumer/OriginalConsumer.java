package com.yanghui.study.kafka.consumer;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.kstream.internals.KStreamImpl;
import org.apache.kafka.streams.processor.ProcessorSupplier;
import org.apache.kafka.streams.state.KeyValueStore;
import org.junit.Test;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Stream;

@Slf4j
public class OriginalConsumer {

    /**
     * 自动提交 偏移量
     */
    @Test
    public void test1(){
        Properties props = new Properties();
        props.put("bootstrap.servers", "192.168.43.151:9092");
        props.put("group.id", "group_02");
        props.put("enable.auto.commit", "true");
        props.put("auto.commit.interval.ms", "1000");
        props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);
        consumer.subscribe(Arrays.asList("test_topic", "my_replicated_topic"));
        int i = 2;
        try {
            do {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
                for (ConsumerRecord<String, String> record : records)
                    log.info("offset = %d, key = %s, value = %s%n", record.offset(), record.key(), record.value());
                --i;
            } while (i == 0);
        }finally {
            consumer.close();
        }
    }


    /**
     * 手动提交 offset
     * 注意：
     * auto.offset.reset设置为 earlier 时，
     * 当Kafka中没有初始offset或如果当前的offset不存在时（例如，该数据被删除了）,
     * 消费组会自动将偏移重置为最早的偏移，从最早的消息开始消费，
     * 否则，会从之前已提交的偏移量(offset)之后继续消费
     *
     * auto.offset.reset设置为 latest 时,
     * 当Kafka中没有初始offset或如果当前的offset不存在时（例如，该数据被删除了）,
     * 消费组会自动将偏移重置为最新偏移，从消费群组启动之后的消息开始消费（对于启动之前的消息，将不会消费），
     * 否则，会接着之前提交的偏移量继续消费
     *
     * 综上所述，auto.offset.reset设置为 earlier 和 latest的区别只在于消费组找不到offset时开始消费的位置，
     * 对于存在offset的消费者组，都是接着之前的偏移量继续消费
     */
    @Test
    public void test2(){
        Properties props = new Properties();
        props.put("bootstrap.servers", "192.168.120.241:9092");
        props.put("group.id", "group_06");
        props.put("enable.auto.commit", "false");
        props.put("session.timeout.ms", "30000");
        props.put("auto.offset.reset", "latest");
        props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);
        consumer.subscribe(Arrays.asList("test_topic"));
        final int minBatchSize = 1;
        List<ConsumerRecord<String, String>> buffer = new ArrayList<>();
        try {
            while (true) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(1000));
                for (ConsumerRecord<String, String> record : records) {
                    buffer.add(record);
                }
                if (buffer.size() >= minBatchSize) {
                    //insertIntoDb(buffer);
                    log.info("消费并处理完成一批数据：" + buffer.toString());
                    consumer.commitSync();
                    buffer.clear();
                }
            }
        }finally {
            consumer.close();
        }
    }

    /**
     *  精确控制偏移量
     */
    @Test
    public void test3(){
        Properties props = new Properties();
        props.put("bootstrap.servers", "192.168.43.151:9092");
        props.put("group.id", "group_07");
        props.put("enable.auto.commit", "false");
        props.put("session.timeout.ms", "30000");
        props.put("auto.offset.reset", "earliest");
        props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);
        consumer.subscribe(Arrays.asList("test_topic"));
        try {
            while(true) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(Long.MAX_VALUE));
                for (TopicPartition partition : records.partitions()) {
                    List<ConsumerRecord<String, String>> partitionRecords = records.records(partition);
                    for (ConsumerRecord<String, String> record : partitionRecords) {
                        System.out.println(record.offset() + ": " + record.value());
                    }
                    long lastOffset = partitionRecords.get(partitionRecords.size() - 1).offset();
                    consumer.commitSync(Collections.singletonMap(partition, new OffsetAndMetadata(lastOffset + 1)));
                }
            }
        } finally {
            consumer.close();
        }
    }

    /**
     *  订阅指定分区
     *
     * 消费者分组仍需要提交offset，只是现在分区的设置只能通过调用assign修改，因为手动分配不会进行分组协调，
     * 因此消费者故障不会引发分区重新平衡。每一个消费者是独立工作的（即使和其他的消费者共享GroupId）。
     * 为了避免offset提交冲突，通常你需要确认每一个consumer实例的gorupId都是唯一的。
     *
     * 注意，手动分配分区（即，assgin）和动态分区分配的订阅topic模式（即，subcribe）不能混合使用。
     */
    @Test
    public void test4(){
        Properties props = new Properties();
        props.put("bootstrap.servers", "192.168.43.151:9092");
        props.put("group.id", "group_06");
        props.put("enable.auto.commit", "false");
        props.put("session.timeout.ms", "30000");
        props.put("auto.offset.reset", "latest");
        props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);
        String topic = "test_topic";
        TopicPartition topicPartition0 = new TopicPartition(topic, 0);
        TopicPartition topicPartition1 = new TopicPartition(topic, 1);
        consumer.assign(Arrays.asList(topicPartition0,topicPartition1));
        try {
            while(true) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(Long.MAX_VALUE));
                for (TopicPartition partition : records.partitions()) {
                    List<ConsumerRecord<String, String>> partitionRecords = records.records(partition);
                    for (ConsumerRecord<String, String> record : partitionRecords) {
                        System.out.println(record.offset() + ": " + record.value());
                    }
                    long lastOffset = partitionRecords.get(partitionRecords.size() - 1).offset();
                    consumer.commitSync(Collections.singletonMap(partition, new OffsetAndMetadata(lastOffset + 1)));
                }
            }
        } finally {
            consumer.close();
        }
    }

    /**
     * 流
     * Kafka Streams
     * 直接将一个主题里的消息传递到另一个主题不做任何处理
     */
    @Test
    public void test5(){
        Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "streams-pipe");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "192.168.43.151:9092");
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());

        final StreamsBuilder builder = new StreamsBuilder();

        builder.stream("streams-plaintext-input").to("streams-pipe-output");

        final Topology topology = builder.build();

        final KafkaStreams streams = new KafkaStreams(topology, props);
        final CountDownLatch latch = new CountDownLatch(1);
        // attach shutdown handler to catch control-c
        Runtime.getRuntime().addShutdownHook(new Thread("streams-shutdown-hook") {
            @Override
            public void run() {
                streams.close();
                latch.countDown();
            }
        });
        try {
            streams.start();
            latch.await();
        } catch (Throwable e) {
            System.exit(1);
        }
        System.exit(0);
    }

    /**
     * Kafka Streams
     * 将一个主题把源流作为输入，并通过按顺序处理源流中的每条消息并将其值字符串分解为一个单词列表，
     * 并生成每个单词作为输出的新消息，从而生成一个名为单词的新流
     */
    @Test
    public void test6(){
        Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "streams-linesplit");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "192.168.43.151:9092");
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());

        final StreamsBuilder builder = new StreamsBuilder();

        KStream<String, String> source = builder.stream("streams-plaintext-input");
        source.flatMapValues( value -> Arrays.asList(value.split("\\W+")))
        .to("streams-linesplit-output");

        final Topology topology = builder.build();
        final KafkaStreams streams = new KafkaStreams(topology, props);
        final CountDownLatch latch = new CountDownLatch(1);
        // attach shutdown handler to catch control-c
        Runtime.getRuntime().addShutdownHook(new Thread("streams-shutdown-hook") {
            @Override
            public void run() {
                streams.close();
                latch.countDown();
            }
        });
        try {
            streams.start();
            latch.await();
        } catch (Throwable e) {
            System.exit(1);
        }
        System.exit(0);

    }

    /**
     * 使用流对单词计数
     *
     * 为了计算单词，我们可以首先修改flatMapValues，将它们全部作为小写字母，
     * 我们必须首先指定我们要关键流的字符串value，即小写单词，用groupBy操作。
     * 该运算符生成一个新的分组流，然后可以由一个计数操作员汇总，该操作员可以在每个分组键上生成一个运行计数，
     * count运算符有Materialized参数，该参数指定运行计数应存储在名为counts-store的状态存储中。 此Counts仓库可以实时查询。
     *
     * 注意，为了从主题streams-wordcount-output读取changelog流，需要将值反序列化设置为org.apache.kafka.common.serialization.LongDeserializer
     */
    @Test
    public void test7(){
        Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "streams-wordcount");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "192.168.43.151:9092");
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());

        final StreamsBuilder builder = new StreamsBuilder();

        KStream<String, String> source = builder.stream("streams-plaintext-input");
        source.flatMapValues(value -> Arrays.asList(value.toLowerCase(Locale.getDefault()).split("\\W+")))
                .groupBy((key, value) -> value)
                .count(Materialized.as("counts-store"))
                .toStream()
                .to("streams-wordcount-output", Produced.with(Serdes.String(), Serdes.Long()));

        final Topology topology = builder.build();
        final KafkaStreams streams = new KafkaStreams(topology, props);
        final CountDownLatch latch = new CountDownLatch(1);
        Runtime.getRuntime().addShutdownHook(new Thread("streams-shutdown-hook") {
            @Override
            public void run() {
                streams.close();
                latch.countDown();
            }
        });
        try {
            streams.start();
            latch.await();
        } catch (Throwable e) {
            System.exit(1);
        }
        System.exit(0);
    }


}
