package com.mycom.myapp.controller;

import com.mycom.myapp.service.kafka.KafkaProducerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/queue")
public class QueueController {

    private final KafkaProducerService kafkaProducerService;

    @PostMapping("/enter")
    public ResponseEntity<String> enterQueue(String userId) {
        kafkaProducerService.sendToQueue(userId);
        return ResponseEntity.ok("대기열 입장에 성공하였습니다.");
    }
}
