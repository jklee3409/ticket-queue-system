package com.mycom.myapp.service;

import com.mycom.myapp.constant.RedisConstant;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class QueueManagerService {

    private final StringRedisTemplate redisTemplate;
    private final RedisScript<List> moveUsersScript;

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
                String key = RedisConstant.ENTRY_TOKEN_KEY + entryToken;
                redisTemplate.opsForValue().set(key, userId, ENTRY_VALID_DURATION);
            });

            log.info("{} users moved from waiting to active queue.", movedUsers.size());
        }

        return movedUsers;
    }
}
