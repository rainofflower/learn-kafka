package com.yanghui.study.kafka.consumer;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.util.concurrent.FailureCallback;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.SuccessCallback;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

@RestController
@RequestMapping("producer")
@Slf4j
public class Producer {

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private Map producerConfigs;

    @RequestMapping("sendMessage")
    public String sendMessage(@RequestBody JSONObject param){
        try{
            /**
             * kafka提供的生产者是线程安全的，在线程之间共享单个生产者实例，通常单例比多个实例要快
             * send方法是异步的，消息添加到缓冲池中,构建一个future实例，立即返回，后台I/0线程负责将这些信息转换成请求发送到集群中
             */
            log.info("准备发送消息");
            ListenableFuture<SendResult<String, String>> future = kafkaTemplate.send(param.getString("topic"), param.getString("content"));
            //LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(5));
            future.addCallback(new SuccessCallback() {
                public void onSuccess(Object o) {
                    log.info("发送成功：{} \n 时间 {}", o,new Date());
                }
            }, new FailureCallback() {
                public void onFailure(Throwable throwable) {
                    log.error(throwable.toString());
                }
            });
            log.info("发送消息线程已执行完，时间 {}",new Date());
            //future.get(5, TimeUnit.SECONDS);
        }catch (Throwable e) {
            log.error(e.toString());
            return "fail";
        }
        return "success";
    }

    public void produce(){
        KafkaProducer kafkaProducer = new KafkaProducer(producerConfigs);

    }

}
