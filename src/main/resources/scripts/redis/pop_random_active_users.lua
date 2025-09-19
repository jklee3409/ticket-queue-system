--  KEYS[1]: 참가열 SET 키
--  ARGV[1]: 제거할 사용자 수 (count)
return redis.call('SPOP', KEYS[1], ARGV[1])