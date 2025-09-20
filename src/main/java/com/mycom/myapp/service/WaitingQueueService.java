package com.mycom.myapp.service;

import com.mycom.myapp.constant.RedisConstant;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class WaitingQueueService {

    private final StringRedisTemplate redisTemplate;

    public void addUserToWaitingQueue(String userId) {
        long timestamp = System.currentTimeMillis();
        redisTemplate.opsForZSet().add(RedisConstant.WAITING_QUEUE_KEY, userId, timestamp);
        log.info("userId: {} Add to Redis WaitingQueue.", userId);
    }

    // 대기열 순위를 한명마다 전부 조회하면 N + 1 문제 발생
    public Long getRank(String userId) {
        Long rank = redisTemplate.opsForZSet().rank(RedisConstant.WAITING_QUEUE_KEY, userId);
        return (rank != null) ? rank : -1;
    }

    // ZSet 은 정렬을 기본 보장 -> 전체를 한번에 조회 -> 그 순서가 곧 대기열 순위
    public List<String> getWaitingUsersInOrder() {
        Set<String> waitingUsersSet = redisTemplate.opsForZSet().range(RedisConstant.WAITING_QUEUE_KEY, 0, -1);

        if (waitingUsersSet == null) return Collections.emptyList();

        return new ArrayList<>(waitingUsersSet);
    }
}
