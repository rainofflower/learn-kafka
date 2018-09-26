package com.yanghui.study.kafka.consumer;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class Consumer1 {
	
	@KafkaListener(id="foo",topics="test")
	public void receive(ConsumerRecord<?, ?> record) {
        System.out.println(record.value());
    }
}
