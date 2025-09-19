package com.mycom.myapp.config;

import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;

@Configuration
public class RedisConfig {

    @Bean
    public RedisScript<List> moveUsersScript() {
        DefaultRedisScript<List> script = new DefaultRedisScript<>();
        script.setLocation(new ClassPathResource("scripts/redis/move_users_from_waiting_to_active.lua"));
        script.setResultType(List.class);
        return script;
    }

    @Bean
    public RedisScript<List> popRandomActiveUsersScript() {
        DefaultRedisScript<List> script = new DefaultRedisScript<>();
        script.setLocation(new ClassPathResource("scripts/redis/pop_random_Active_users.lua"));
        script.setResultType(List.class);
        return script;
    }
}
