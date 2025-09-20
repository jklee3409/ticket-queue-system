package com.mycom.myapp.controller;

import com.mycom.myapp.service.QueueManagerService;
import com.mycom.myapp.service.WaitingQueueService;
import com.mycom.myapp.service.kafka.KafkaProducerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/queue")
public class QueueController {

    private final KafkaProducerService kafkaProducerService;
    private final QueueManagerService queueManagerService;
    private final WaitingQueueService waitingQueueService;

    @PostMapping("/enter")
    public ResponseEntity<String> enterQueue(@RequestParam String userId) {
        kafkaProducerService.sendToQueue(userId);
        return ResponseEntity.ok("대기열 입장에 성공하였습니다.");
    }

    @PostMapping("/reserve/{userId}/{entryToken}")
    public ResponseEntity<String> reserve(
            @PathVariable String userId,
            @PathVariable String entryToken) {
        boolean isValid = queueManagerService.isValidEntryToken(entryToken);

        if (isValid) return ResponseEntity.ok("예약에 성공하였습니다.");
        else return ResponseEntity.badRequest().body("잘못된 접근입니다.");
    }

    @GetMapping("/rank/{userId}")
    public ResponseEntity<Long> getRank(@PathVariable String userId) {
        Long rank = waitingQueueService.getRank(userId);
        return ResponseEntity.ok(rank);
    }
}
