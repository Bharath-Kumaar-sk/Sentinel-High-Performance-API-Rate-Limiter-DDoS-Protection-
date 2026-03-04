--initialize variables to access keys
--these are user specific
local tokens_key = KEYS[1]
local time_key = KEYS[2]
local strikes_key = KEYS[3]
local ban_key = KEYS[4]

--Get the values for those keys
local current_tokens = redis.call("GET", tokens_key)
local last_refill_time = redis.call("GET", time_key)
local current_strikes = redis.call("GET", strikes_key)
local ban_time = redis.call("GET", ban_key)


if  ban_time ~= false and ban_time ~= nil then
    return 0
end

--configuration/logic needed variables
local maxAmount = tonumber(ARGV[1])
local refill_rate = tonumber(ARGV[2])
local current_time = tonumber(ARGV[3])
local maxStrikes = tonumber(ARGV[4])
local ban_duration = tonumber(ARGV[5])


--If user is new max amount is current amount, last_refill_time = current_time
if current_tokens == false or current_tokens == nil then
    current_tokens = tonumber(ARGV[1])
    last_refill_time = tonumber(ARGV[3])
    current_strikes = tonumber(0)
    ban_time = false
    --else we just ge the Value from the existing key
else
    current_tokens = tonumber(current_tokens)
    last_refill_time = tonumber(last_refill_time)
    current_strikes = tonumber(current_strikes) or 0
end

--Lazy refill
local refill_bucket = (current_time - last_refill_time)*refill_rate
--update the current tokens (Ceiling is the MaxAmount)
current_tokens = math.min(maxAmount, current_tokens + refill_bucket)

--true if tokens available or else false
local reqAllowed = 0
if current_tokens >= 1 then
    current_tokens = current_tokens - 1
    redis.call("SET", strikes_key, 0, "EX", 86400)
    reqAllowed = 1


else
    current_strikes = tonumber(redis.call("INCR", strikes_key))
    redis.call("EXPIRE", strikes_key, 86400)

    if current_strikes >= maxStrikes then
        redis.call("SET", ban_key, "true", "EX", ban_duration)
    end
end


redis.call("SET", tokens_key, current_tokens, "EX", 86400)
redis.call("SET", time_key, current_time, "EX", 86400)

return reqAllowed