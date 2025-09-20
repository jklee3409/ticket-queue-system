package com.mycom.myapp.service;

import com.mycom.myapp.constant.RedisConstant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EntryQueueService {

    private final StringRedisTemplate redisTemplate;
    private final RedisScript<List> popRandomActiveUsersScript;

    public Long getActiveUserCount() {
        return redisTemplate.opsForSet().size(RedisConstant.ACTIVE_USERS_KEY);
    }

    public void popRandomActiveUsers() {
        int count = (int) (Math.random() * 10) + 1;

        List<String> keys = Arrays.asList(
                RedisConstant.ACTIVE_USERS_KEY,
                RedisConstant.USER_TOKEN_KEY,
                RedisConstant.ENTRY_TOKEN_KEY
        );

        redisTemplate.execute(
                popRandomActiveUsersScript,
                keys,
                String.valueOf(count)
        );
    }
}
