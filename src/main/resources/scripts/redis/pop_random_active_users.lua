-- KEYS[1]: 참가열 Set 키
-- KEYS[2]: 유저-토큰 맵핑 키
-- KEYS[3]: 토큰-유저 맵핑 키
-- ARGV[1]: 제거할 사용자 수 (count)

-- 1. 참가열 Set에서 지정된 수만큼 랜덤하게 사용자를 꺼내온다.
local users = redis.call('SPOP', KEYS[1], ARGV[1])

-- SPOP 결과가 비어있을 수 있으므로 확인
if #users == 0 then
    return {}
end

-- 2. 삭제할 키들을 담을 리스트를 준비한다.
local keys_to_delete = {}

-- 3. 꺼내온 사용자 ID 목록을 순회한다.
for i, user_id in ipairs(users) do
    local user_key = KEYS[2] .. user_id

    -- 4. user_id로 토큰을 조회한다.
    local token = redis.call('GET', user_key)

    if token then
        local entry_key = KEYS[3] .. token
        -- 5. 삭제 리스트에 'user-token' 키와 'entry-token' 키를 모두 추가한다.
        table.insert(keys_to_delete, user_key)
        table.insert(keys_to_delete, entry_key)
    end
end

-- 6. 삭제할 키가 있다면, DEL 명령어로 한번에 모두 삭제한다. (원자성 보장)
if #keys_to_delete > 0 then
    redis.call('DEL', unpack(keys_to_delete))
end

-- 7. 실제로 Set에서 제거된 사용자 목록을 반환한다.
return users