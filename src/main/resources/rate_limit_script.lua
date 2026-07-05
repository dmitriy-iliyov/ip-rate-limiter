local ip_key = KEYS[1]
local blocked_ip_key = KEYS[2]
local observe_ttl = tonumber(ARGV[1])
local lock_ttl = tonumber(ARGV[2])
local max_attempt = tonumber(ARGV[3])

if redis.call("EXISTS", blocked_ip_key) == 1 then
    return 0
end

local attempt = redis.call("INCR", ip_key)

if attempt == 1 then
    redis.call("EXPIRE", ip_key, observe_ttl)
end

if attempt <= max_attempt then
    return 1
else
    redis.call("SET", blocked_ip_key, "blocked", "NX", "EX", lock_ttl)
    return 0
end
