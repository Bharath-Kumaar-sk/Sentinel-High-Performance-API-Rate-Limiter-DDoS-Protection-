--initialize variables to access keys
local tokens_key = KEYS[1]
local time_key = KEYS[2]

--Get the values for those keys
local current_tokens = redis.call("GET", tokens_key)
local last_refill_time = redis.call("GET", time_key)

local maxAmount = tonumber(ARGV[1])
local refill_rate = tonumber(ARGV[2])
local current_time = tonumber(ARGV[3])

--If user is new max amount is current amount, last_refill_time = current_time
if current_tokens == false or current_tokens == nil then
    current_tokens = tonumber(ARGV[1])
    last_refill_time = tonumber(ARGV[3])
    --else we just ge the Value from the existing key
else
    current_tokens = tonumber(current_tokens)
    last_refill_time = tonumber(last_refill_time)
end

--Lazy refill
local refill_bucket = (current_time - last_refill_time)*refill_rate
--update the current tokens (Ceiling is the MaxAmount)
current_tokens = math.min(maxAmount, current_tokens + refill_bucket)

--true if tokens available or else false
local reqAllowed = 0
if current_tokens >= 1 then
    current_tokens = current_tokens - 1
    reqAllowed = 1
end

redis.call("SET", tokens_key, current_tokens, "EX", 86400)
redis.call("SET", time_key, current_time, "EX", 86400)

return reqAllowed