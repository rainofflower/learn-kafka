package learnkafka.test;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.junit4.SpringRunner;

import com.yanghui.study.kafka.KafkaApplication;


@RunWith(SpringRunner.class)
@SpringBootTest(classes=KafkaApplication.class)
public class TestClass {

	@Autowired
	private KafkaTemplate<Integer, String> kafkaTemplate;
	
	@Test
	public void test1() {
		String data = "Hello Kafka,thanks for your kind!";
		kafkaTemplate.send("test",data);
	}
}
