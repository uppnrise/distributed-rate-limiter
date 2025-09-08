-- Redis Lua script for atomic token bucket operations
-- KEYS[1]: bucket key
-- ARGV[1]: capacity (max tokens)
-- ARGV[2]: refill rate (tokens per second)
-- ARGV[3]: tokens to consume
-- ARGV[4]: current time in milliseconds

local bucket_key = KEYS[1]
local capacity = tonumber(ARGV[1])
local refill_rate = tonumber(ARGV[2])
local tokens_to_consume = tonumber(ARGV[3])
local current_time = tonumber(ARGV[4])

-- Get current bucket state or initialize
local bucket_data = redis.call('HMGET', bucket_key, 'tokens', 'last_refill')
local current_tokens = tonumber(bucket_data[1])
local last_refill = tonumber(bucket_data[2])

-- Initialize if this is the first access
if current_tokens == nil then
    current_tokens = capacity
end
if last_refill == nil then
    last_refill = current_time
end

-- Calculate tokens to add based on time elapsed
local time_elapsed = current_time - last_refill
local tokens_to_add = math.floor(time_elapsed / 1000 * refill_rate)

-- Update token count, capped at capacity
current_tokens = math.min(capacity, current_tokens + tokens_to_add)

-- Check if we can consume the requested tokens
local success = false
if tokens_to_consume > 0 then
    if current_tokens >= tokens_to_consume then
        current_tokens = current_tokens - tokens_to_consume
        success = true
    end
elseif tokens_to_consume == 0 then
    -- This is a query for current state, no consumption
    success = false  -- Don't report success for queries
end

-- Only update state if we're actually consuming tokens
if tokens_to_consume >= 0 then
    -- Update bucket state in Redis
    redis.call('HMSET', bucket_key, 
        'tokens', current_tokens,
        'last_refill', current_time,
        'capacity', capacity,
        'refill_rate', refill_rate)

    -- Set expiration to cleanup after inactivity (24 hours)
    redis.call('EXPIRE', bucket_key, 86400)
end

-- Return result: {success, current_tokens, capacity, refill_rate, last_refill}
return {success and 1 or 0, current_tokens, capacity, refill_rate, current_time}