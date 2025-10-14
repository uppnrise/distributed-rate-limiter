-- Redis Lua script for atomic leaky bucket operations
-- KEYS[1]: bucket key
-- ARGV[1]: queue capacity (max requests that can be queued)
-- ARGV[2]: leak rate per second (requests processed per second)
-- ARGV[3]: tokens to consume (usually 1 for request-based limiting)
-- ARGV[4]: current time in milliseconds
-- ARGV[5]: max queue time in milliseconds (timeout for queued requests)

local queue_key = KEYS[1]
local capacity = tonumber(ARGV[1])
local leak_rate = tonumber(ARGV[2])
local tokens_to_consume = tonumber(ARGV[3])
local current_time = tonumber(ARGV[4])
local max_queue_time = tonumber(ARGV[5])

-- Ensure we have valid inputs
if capacity <= 0 or leak_rate <= 0 or tokens_to_consume < 0 or max_queue_time <= 0 then
    return {-1, -1, capacity, leak_rate, current_time, 0}
end

-- Keys for queue and metadata
local metadata_key = queue_key .. ':meta'
local queue_list_key = queue_key .. ':queue'

-- Get current metadata
local metadata = redis.call('HMGET', metadata_key, 'last_leak_time', 'queue_size')
local last_leak_time = metadata[1]
local queue_size = metadata[2]

-- Initialize if this is the first access
if last_leak_time == false or last_leak_time == nil then
    last_leak_time = current_time
else
    last_leak_time = tonumber(last_leak_time)
end

if queue_size == false or queue_size == nil then
    queue_size = 0
else
    queue_size = tonumber(queue_size)
end

-- Clean up expired requests from the front of the queue
local cleanup_count = 0
while queue_size > 0 do
    -- Peek at the oldest request in the queue
    local oldest_request = redis.call('LINDEX', queue_list_key, 0)
    if oldest_request then
        local oldest_time = tonumber(oldest_request)
        if oldest_time and (current_time - oldest_time) > max_queue_time then
            -- Remove expired request
            redis.call('LPOP', queue_list_key)
            queue_size = queue_size - 1
            cleanup_count = cleanup_count + 1
        else
            break -- No more expired requests
        end
    else
        break
    end
end

-- Process requests at leak rate (simulate draining)
local time_elapsed = math.max(0, current_time - last_leak_time)
local tokens_to_process = math.floor(time_elapsed / 1000 * leak_rate)

if tokens_to_process > 0 and queue_size > 0 then
    -- Process (remove) requests from the front of the queue
    local processed_count = math.min(tokens_to_process, queue_size)
    for i = 1, processed_count do
        redis.call('LPOP', queue_list_key)
    end
    queue_size = queue_size - processed_count
    last_leak_time = current_time
end

-- Handle new request
local success = 0
local estimated_wait = 0

if tokens_to_consume == 0 then
    -- This is a query for current state, no consumption
    success = 0
elseif tokens_to_consume > 0 then
    -- Check if queue has capacity
    if queue_size + tokens_to_consume <= capacity then
        -- Add request(s) to queue
        for i = 1, tokens_to_consume do
            redis.call('RPUSH', queue_list_key, current_time)
        end
        queue_size = queue_size + tokens_to_consume
        success = 1
        
        -- Calculate estimated wait time
        estimated_wait = math.floor(queue_size / leak_rate * 1000) -- Convert to milliseconds
    else
        -- Queue full, reject
        success = 0
        estimated_wait = -1 -- Indicate rejection due to capacity
    end
end

-- Update metadata
redis.call('HMSET', metadata_key, 
    'last_leak_time', last_leak_time,
    'queue_size', queue_size,
    'capacity', capacity,
    'leak_rate', leak_rate)

-- Set expiration for cleanup after inactivity (24 hours)
redis.call('EXPIRE', metadata_key, 86400)
redis.call('EXPIRE', queue_list_key, 86400)

-- Return result: {success, queue_size, capacity, leak_rate, last_leak_time, estimated_wait_ms}
return {success, queue_size, capacity, leak_rate, last_leak_time, estimated_wait}