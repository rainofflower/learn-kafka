package learnkafka.test;

import com.yanghui.study.kafka.config.KafkaProducerConfig;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes= KafkaProducerConfig.class)
@EnableKafka
public class TestClass {

	@Value("${kafka.topic1}")
	private String topic;

	@Autowired
	private KafkaTemplate<Integer, String> kafkaTemplate;
	
	@Test
	public void test1() {
		String data = "yanghui1122";
		kafkaTemplate.send( topic, data);
	}
}
