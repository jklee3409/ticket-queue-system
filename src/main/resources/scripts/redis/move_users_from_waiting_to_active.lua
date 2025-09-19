-- KEYS[1]: 대기열 ZSET 키
-- KEYS[2]: 참가열 SET 키
-- ARGV[1]: 이동시킬 최대 사용자 수 (count)

local users_to_move = redis.call('ZRANGE', KEYS[1], 0, ARGV[1] - 1)

if #users_to_move > 0 then
    -- unpack()은 Lua 테이블의 원소를 개별 인자로 풀어주는 함수이다.
    redis.call('ZREM', KEYS[1], unpack(users_to_move))
    redis.call('SADD', KEYS[2], unpack(users_to_move))
end

return users_to_move