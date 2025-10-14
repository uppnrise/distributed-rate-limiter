-- Redis Lua script for atomic fixed window operations
-- KEYS[1]: window key
-- ARGV[1]: capacity (max requests per window)
-- ARGV[2]: window_duration (window duration in milliseconds)
-- ARGV[3]: tokens to consume
-- ARGV[4]: current time in milliseconds

local window_key = KEYS[1]
local capacity = tonumber(ARGV[1])
local window_duration = tonumber(ARGV[2])
local tokens_to_consume = tonumber(ARGV[3])
local current_time = tonumber(ARGV[4])

-- Ensure we have valid inputs
if capacity <= 0 or window_duration <= 0 or tokens_to_consume < 0 then
    return {-1, -1, capacity, window_duration, current_time}
end

-- Get current window state
local window_data = redis.call('HMGET', window_key, 'count', 'window_start')
local current_count = window_data[1]
local window_start = window_data[2]

-- Calculate current window start time
local current_window_start = math.floor(current_time / window_duration) * window_duration

-- Initialize or reset window if needed
if window_start == false or window_start == nil or tonumber(window_start) ~= current_window_start then
    -- New window, reset counter
    current_count = 0
    window_start = current_window_start
else
    current_count = tonumber(current_count)
    window_start = tonumber(window_start)
end

-- Check if we can consume the requested tokens
local success = 0
local remaining_tokens = capacity - current_count

if tokens_to_consume == 0 then
    -- This is a query for current state, no consumption
    success = 0
elseif tokens_to_consume > 0 and remaining_tokens >= tokens_to_consume then
    current_count = current_count + tokens_to_consume
    remaining_tokens = capacity - current_count
    success = 1
else
    -- Cannot consume, not enough capacity
    success = 0
end

-- Update window state
redis.call('HMSET', window_key, 
    'count', current_count,
    'window_start', window_start,
    'capacity', capacity,
    'window_duration', window_duration)

-- Set expiration to cleanup after window duration + buffer
local expire_time = math.ceil(window_duration / 1000) + 3600  -- Window duration + 1 hour buffer
redis.call('EXPIRE', window_key, expire_time)

-- Return result: {success, remaining_tokens, current_count, window_start, current_time}
return {success, remaining_tokens, current_count, window_start, current_time}