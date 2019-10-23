package com.yanghui.study.kafka.consumer;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class Consumer {

	@KafkaListener(topics = "streams-wordcount-output")//${kafka.topic1}
	public void receive(ConsumerRecords<String, String> records) {
		for(ConsumerRecord<String,String> record : records){
			log.info("\n收到消息 \n主题：{}，分区：{}\n内容：{},偏移量：{}",record.topic(),record.partition(),record.value(),record.offset());
		}
    }
}
