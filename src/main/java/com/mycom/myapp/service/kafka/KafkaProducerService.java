package com.mycom.myapp.service.kafka;

import com.mycom.myapp.constant.KafkaConstant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaProducerService {

    private final KafkaTemplate<String, String> kafkaTemplate;

    public void sendToQueue(String userId) {
        log.info("userID: {} Sending to Kafka queue", userId);
        kafkaTemplate.send(KafkaConstant.QUEUE_TOPIC, userId);
    }
}
