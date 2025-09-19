package com.mycom.myapp.service.kafka;

import com.mycom.myapp.constant.KafkaConstant;
import com.mycom.myapp.service.WaitingQueueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaConsumerService {

    private final WaitingQueueService waitingQueueService;

    @KafkaListener(topics = KafkaConstant.QUEUE_TOPIC, groupId = "ticket-queue-group")
    public void consume(String userId) {
        log.info("Consumed from Kafka queue. userId: {}", userId);
        waitingQueueService.addUserToWaitingQueue(userId);
    }
}
