package com.mycom.myapp.service;

import com.mycom.myapp.constant.RedisConstant;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.StringRedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class QueueManagerService {

    private final StringRedisTemplate redisTemplate;
    private final RedisScript<List> moveUsersScript;
    private final SseService sseService;

    private static final Duration ENTRY_VALID_DURATION = Duration.ofMinutes(3);

    public List<String> moveTopNUsersToEntryQueue(long count) {
        if (count <= 0) return Collections.emptyList();

        @SuppressWarnings("unchecked")
        List<String> movedUsers = (List<String>) redisTemplate.execute(
                moveUsersScript,
                List.of(RedisConstant.WAITING_QUEUE_KEY, RedisConstant.ACTIVE_USERS_KEY),
                String.valueOf(count)
        );

        if (!movedUsers.isEmpty()) {

            movedUsers.forEach(userId -> {
                String entryToken = UUID.randomUUID().toString();
                String entryKey = RedisConstant.ENTRY_TOKEN_KEY + entryToken;
                String userKey = RedisConstant.USER_TOKEN_KEY + userId;

                long ttlSeconds = ENTRY_VALID_DURATION.getSeconds();
                long expiresAt = System.currentTimeMillis() + ttlSeconds * 1000;

                // 양방향 맵핑 설정
                redisTemplate.executePipelined((RedisCallback<?>) connection -> {
                    StringRedisConnection redisConnection = (StringRedisConnection) connection;
                    redisConnection.setEx(entryKey, ttlSeconds, userId);
                    redisConnection.setEx(userKey, ttlSeconds, entryToken);
                    return null;
                });

                // SSE 로 토큰 및 만료 시간 전달
                Map<String, Object> payload = new HashMap<>();
                payload.put("entryToken", entryToken);
                payload.put("expiresAt", expiresAt);
                sseService.sentToClient(userId, "ENTRY_TOKEN", payload);
            });

            log.info("{} users moved from waiting to active queue.", movedUsers.size());
        }

        return movedUsers;
    }

    public boolean isValidEntryToken(String entryToken) {
        String key = RedisConstant.ENTRY_TOKEN_KEY + entryToken;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }
}
