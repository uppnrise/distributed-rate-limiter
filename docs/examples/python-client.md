# Python Client Example

This example shows how to integrate with the Distributed Rate Limiter using Python.

## Dependencies

```bash
pip install requests
# or for async support:
pip install aiohttp
```

## Simple Client

```python
import requests
import time
from typing import Optional, Dict, Any
from dataclasses import dataclass

@dataclass
class RateLimitResult:
    allowed: bool
    key: str
    tokens_requested: int

class RateLimiterClient:
    """Simple synchronous rate limiter client."""
    
    def __init__(self, base_url: str = "http://localhost:8080", api_key: Optional[str] = None):
        self.base_url = base_url.rstrip('/')
        self.api_key = api_key
        self.session = requests.Session()
        
        # Set default timeout
        self.session.timeout = 10
        
    def check_rate_limit(self, key: str, tokens: int = 1) -> RateLimitResult:
        """Check if request is allowed under rate limit."""
        url = f"{self.base_url}/api/ratelimit/check"
        
        payload = {
            "key": key,
            "tokens": tokens
        }
        
        if self.api_key:
            payload["apiKey"] = self.api_key
            
        try:
            response = self.session.post(url, json=payload)
            
            # Handle different response codes
            if response.status_code == 200:
                data = response.json()
                return RateLimitResult(
                    allowed=data["allowed"],
                    key=data["key"],
                    tokens_requested=data["tokensRequested"]
                )
            elif response.status_code == 429:
                data = response.json()
                return RateLimitResult(
                    allowed=False,
                    key=data["key"],
                    tokens_requested=data["tokensRequested"]
                )
            elif response.status_code == 401:
                raise ValueError("Invalid API key")
            elif response.status_code == 403:
                raise ValueError("IP address not allowed")
            else:
                response.raise_for_status()
                
        except requests.exceptions.RequestException as e:
            # On network error, fail open (allow request)
            print(f"Rate limiter error: {e}, allowing request")
            return RateLimitResult(allowed=True, key=key, tokens_requested=tokens)

# Usage example
def main():
    client = RateLimiterClient(api_key="your-api-key")
    
    # Check rate limit for a user
    user_id = "user123"
    result = client.check_rate_limit(f"user:{user_id}", tokens=1)
    
    if result.allowed:
        print(f"Request allowed for {result.key}")
        # Process the request
    else:
        print(f"Rate limit exceeded for {result.key}")
        # Return error or implement backoff

if __name__ == "__main__":
    main()
```

## Async Client

```python
import aiohttp
import asyncio
from typing import Optional

class AsyncRateLimiterClient:
    """Asynchronous rate limiter client."""
    
    def __init__(self, base_url: str = "http://localhost:8080", api_key: Optional[str] = None):
        self.base_url = base_url.rstrip('/')
        self.api_key = api_key
        
    async def check_rate_limit(self, key: str, tokens: int = 1) -> RateLimitResult:
        """Async rate limit check."""
        url = f"{self.base_url}/api/ratelimit/check"
        
        payload = {
            "key": key,
            "tokens": tokens
        }
        
        if self.api_key:
            payload["apiKey"] = self.api_key
            
        timeout = aiohttp.ClientTimeout(total=10)
        
        try:
            async with aiohttp.ClientSession(timeout=timeout) as session:
                async with session.post(url, json=payload) as response:
                    if response.status == 200:
                        data = await response.json()
                        return RateLimitResult(
                            allowed=data["allowed"],
                            key=data["key"],
                            tokens_requested=data["tokensRequested"]
                        )
                    elif response.status == 429:
                        data = await response.json()
                        return RateLimitResult(
                            allowed=False,
                            key=data["key"],
                            tokens_requested=data["tokensRequested"]
                        )
                    elif response.status in [401, 403]:
                        error_text = await response.text()
                        raise ValueError(f"Authentication/Authorization error: {error_text}")
                    else:
                        response.raise_for_status()
                        
        except asyncio.TimeoutError:
            print(f"Rate limiter timeout for {key}, allowing request")
            return RateLimitResult(allowed=True, key=key, tokens_requested=tokens)
        except aiohttp.ClientError as e:
            print(f"Rate limiter error: {e}, allowing request")
            return RateLimitResult(allowed=True, key=key, tokens_requested=tokens)

# Async usage example
async def async_main():
    client = AsyncRateLimiterClient(api_key="your-api-key")
    
    # Check multiple rate limits concurrently
    keys = ["user:123", "user:456", "api:endpoint1"]
    
    tasks = [client.check_rate_limit(key) for key in keys]
    results = await asyncio.gather(*tasks)
    
    for result in results:
        print(f"Key: {result.key}, Allowed: {result.allowed}")

if __name__ == "__main__":
    asyncio.run(async_main())
```

## Flask Integration

```python
from flask import Flask, request, jsonify
from functools import wraps
import time

app = Flask(__name__)
rate_limiter = RateLimiterClient(api_key="your-api-key")

def rate_limit(key_func=None, tokens=1, backoff_seconds=1):
    """Decorator for rate limiting Flask routes."""
    def decorator(f):
        @wraps(f)
        def decorated_function(*args, **kwargs):
            # Determine rate limit key
            if key_func:
                key = key_func()
            else:
                # Default: use IP address
                key = f"ip:{request.remote_addr}"
            
            # Check rate limit
            result = rate_limiter.check_rate_limit(key, tokens)
            
            if not result.allowed:
                return jsonify({
                    "error": "Rate limit exceeded",
                    "key": result.key,
                    "retry_after": backoff_seconds
                }), 429
            
            return f(*args, **kwargs)
        return decorated_function
    return decorator

@app.route("/api/users/<user_id>")
@rate_limit(key_func=lambda: f"user:{request.view_args['user_id']}", tokens=1)
def get_user(user_id):
    """Example endpoint with rate limiting."""
    return jsonify({"user_id": user_id, "name": "John Doe"})

@app.route("/api/search")
@rate_limit(key_func=lambda: f"search:{request.remote_addr}", tokens=5)
def search():
    """Search endpoint requiring 5 tokens."""
    query = request.args.get("q", "")
    return jsonify({"query": query, "results": []})

if __name__ == "__main__":
    app.run(debug=True)
```

## Configuration

Environment variables:

```bash
export RATE_LIMITER_URL=http://localhost:8080
export RATE_LIMITER_API_KEY=your-api-key
```

Configuration class:

```python
import os

class RateLimiterConfig:
    """Configuration for rate limiter client."""
    
    def __init__(self):
        self.url = os.getenv("RATE_LIMITER_URL", "http://localhost:8080")
        self.api_key = os.getenv("RATE_LIMITER_API_KEY")
        self.timeout = int(os.getenv("RATE_LIMITER_TIMEOUT", "10"))
        self.fail_open = os.getenv("RATE_LIMITER_FAIL_OPEN", "true").lower() == "true"

# Usage
config = RateLimiterConfig()
client = RateLimiterClient(config.url, config.api_key)
```

## Error Handling and Resilience

```python
import logging
from functools import wraps
from time import sleep

logger = logging.getLogger(__name__)

class ResilientRateLimiterClient(RateLimiterClient):
    """Rate limiter client with retry and circuit breaker logic."""
    
    def __init__(self, *args, max_retries=3, fail_open=True, **kwargs):
        super().__init__(*args, **kwargs)
        self.max_retries = max_retries
        self.fail_open = fail_open
        self.failure_count = 0
        self.last_failure_time = 0
        self.circuit_open_duration = 60  # seconds
        
    def check_rate_limit(self, key: str, tokens: int = 1) -> RateLimitResult:
        """Check rate limit with retry and circuit breaker."""
        
        # Circuit breaker check
        if self._is_circuit_open():
            logger.warning("Circuit breaker open, failing open")
            return RateLimitResult(allowed=True, key=key, tokens_requested=tokens)
        
        for attempt in range(self.max_retries + 1):
            try:
                result = super().check_rate_limit(key, tokens)
                self._record_success()
                return result
                
            except Exception as e:
                logger.error(f"Rate limiter attempt {attempt + 1} failed: {e}")
                
                if attempt < self.max_retries:
                    sleep(2 ** attempt)  # Exponential backoff
                else:
                    self._record_failure()
                    
                    if self.fail_open:
                        logger.warning("All retries failed, failing open")
                        return RateLimitResult(allowed=True, key=key, tokens_requested=tokens)
                    else:
                        raise
                        
    def _is_circuit_open(self) -> bool:
        """Check if circuit breaker is open."""
        if self.failure_count >= 5:  # Circuit opens after 5 failures
            if time.time() - self.last_failure_time < self.circuit_open_duration:
                return True
            else:
                # Try to close circuit
                self.failure_count = 0
                
        return False
        
    def _record_success(self):
        """Record successful request."""
        self.failure_count = 0
        
    def _record_failure(self):
        """Record failed request."""
        self.failure_count += 1
        self.last_failure_time = time.time()
```