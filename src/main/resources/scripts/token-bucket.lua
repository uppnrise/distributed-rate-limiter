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

-- Ensure we have valid inputs
if capacity <= 0 or refill_rate < 0 or tokens_to_consume < 0 then
    return {-1, -1, capacity, refill_rate, current_time}
end

-- Get current bucket state
local bucket_data = redis.call('HMGET', bucket_key, 'tokens', 'last_refill')
local current_tokens = bucket_data[1]
local last_refill = bucket_data[2]

-- Initialize if this is the first access
if current_tokens == false or current_tokens == nil then
    current_tokens = capacity
else
    current_tokens = tonumber(current_tokens)
end

if last_refill == false or last_refill == nil then
    last_refill = current_time
else
    last_refill = tonumber(last_refill)
end

-- Calculate tokens to add based on time elapsed
local time_elapsed = math.max(0, current_time - last_refill)
local tokens_to_add = math.floor(time_elapsed / 1000 * refill_rate)

-- Update token count, capped at capacity
current_tokens = math.min(capacity, current_tokens + tokens_to_add)

-- Check if we can consume the requested tokens
local success = 0
if tokens_to_consume == 0 then
    -- This is a query for current state, no consumption
    success = 0
elseif tokens_to_consume > 0 and current_tokens >= tokens_to_consume then
    current_tokens = current_tokens - tokens_to_consume
    success = 1
end

-- Always update state to refresh last_refill time
redis.call('HMSET', bucket_key, 
    'tokens', current_tokens,
    'last_refill', current_time,
    'capacity', capacity,
    'refill_rate', refill_rate)

-- Set expiration to cleanup after inactivity (24 hours)
redis.call('EXPIRE', bucket_key, 86400)

-- Return result: {success, current_tokens, capacity, refill_rate, last_refill}
return {success, current_tokens, capacity, refill_rate, current_time}