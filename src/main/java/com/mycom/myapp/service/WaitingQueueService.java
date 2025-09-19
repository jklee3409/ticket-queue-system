package com.mycom.myapp.service;

import com.mycom.myapp.constant.RedisConstant;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
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

    public Long getRank(String userId) {
        Long rank = redisTemplate.opsForZSet().rank(RedisConstant.WAITING_QUEUE_KEY, userId);
        return (rank != null) ? rank : -1;
    }

    public String moveToActiveQueue() {
        Set<String> users = redisTemplate.opsForZSet().range(RedisConstant.WAITING_QUEUE_KEY, 0, 0);
        if (users == null || users.isEmpty()) return null;

        String userIdToMoveActive = users.iterator().next();

        redisTemplate.opsForZSet().remove(RedisConstant.WAITING_QUEUE_KEY, userIdToMoveActive);
        redisTemplate.opsForSet().add(RedisConstant.ACTIVE_USERS_KEY, userIdToMoveActive);

        return userIdToMoveActive;
    }
}
