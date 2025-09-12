# Node.js Client Example

This example demonstrates how to integrate the Distributed Rate Limiter with Node.js applications.

## Dependencies

```bash
npm install axios express
# or with fetch (Node 18+)
# No additional dependencies needed
```

## Simple Client

```javascript
const axios = require('axios');

class RateLimiterClient {
    constructor(baseUrl = 'http://localhost:8080', apiKey = null) {
        this.baseUrl = baseUrl.replace(/\/$/, '');
        this.apiKey = apiKey;
        this.client = axios.create({
            baseURL: this.baseUrl,
            timeout: 10000,
            headers: {
                'Content-Type': 'application/json'
            }
        });
    }

    async checkRateLimit(key, tokens = 1) {
        const payload = {
            key,
            tokens
        };

        if (this.apiKey) {
            payload.apiKey = this.apiKey;
        }

        try {
            const response = await this.client.post('/api/ratelimit/check', payload);
            
            return {
                allowed: response.data.allowed,
                key: response.data.key,
                tokensRequested: response.data.tokensRequested
            };
        } catch (error) {
            if (error.response) {
                // Handle HTTP errors
                const status = error.response.status;
                
                if (status === 429) {
                    return {
                        allowed: false,
                        key: error.response.data.key,
                        tokensRequested: error.response.data.tokensRequested
                    };
                } else if (status === 401) {
                    throw new Error('Invalid API key');
                } else if (status === 403) {
                    throw new Error('IP address not allowed');
                }
            }
            
            // On network or other errors, fail open
            console.warn(`Rate limiter error: ${error.message}, allowing request`);
            return {
                allowed: true,
                key,
                tokensRequested: tokens
            };
        }
    }
}

module.exports = RateLimiterClient;
```

## Express.js Middleware

```javascript
const express = require('express');
const RateLimiterClient = require('./rate-limiter-client');

const app = express();
const rateLimiter = new RateLimiterClient('http://localhost:8080', 'your-api-key');

// Rate limiting middleware
function createRateLimitMiddleware(options = {}) {
    const {
        keyGenerator = (req) => `ip:${req.ip}`,
        tokens = 1,
        skipSuccessfulRequests = false,
        skipFailedRequests = false
    } = options;

    return async (req, res, next) => {
        try {
            const key = typeof keyGenerator === 'function' 
                ? keyGenerator(req) 
                : keyGenerator;
            
            const result = await rateLimiter.checkRateLimit(key, tokens);
            
            if (!result.allowed) {
                return res.status(429).json({
                    error: 'Rate limit exceeded',
                    key: result.key,
                    retryAfter: 1
                });
            }
            
            // Add rate limit info to response headers
            res.set('X-RateLimit-Key', result.key);
            res.set('X-RateLimit-Tokens', result.tokensRequested.toString());
            
            next();
        } catch (error) {
            console.error('Rate limit middleware error:', error);
            // Fail open - allow the request to continue
            next();
        }
    };
}

// Global rate limiting
app.use(createRateLimitMiddleware({
    keyGenerator: (req) => `global:${req.ip}`,
    tokens: 1
}));

// User-specific rate limiting
app.get('/api/users/:userId', 
    createRateLimitMiddleware({
        keyGenerator: (req) => `user:${req.params.userId}`,
        tokens: 1
    }),
    (req, res) => {
        res.json({ 
            userId: req.params.userId, 
            name: 'John Doe' 
        });
    }
);

// Expensive operation requiring more tokens
app.post('/api/search',
    createRateLimitMiddleware({
        keyGenerator: (req) => `search:${req.ip}`,
        tokens: 5
    }),
    (req, res) => {
        res.json({ 
            query: req.body.query,
            results: [] 
        });
    }
);

app.listen(3000, () => {
    console.log('Server running on port 3000');
});
```

## Modern Fetch API Client

```javascript
class ModernRateLimiterClient {
    constructor(baseUrl = 'http://localhost:8080', apiKey = null) {
        this.baseUrl = baseUrl.replace(/\/$/, '');
        this.apiKey = apiKey;
    }

    async checkRateLimit(key, tokens = 1) {
        const payload = {
            key,
            tokens
        };

        if (this.apiKey) {
            payload.apiKey = this.apiKey;
        }

        const controller = new AbortController();
        const timeoutId = setTimeout(() => controller.abort(), 10000);

        try {
            const response = await fetch(`${this.baseUrl}/api/ratelimit/check`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(payload),
                signal: controller.signal
            });

            clearTimeout(timeoutId);

            if (response.ok || response.status === 429) {
                const data = await response.json();
                return {
                    allowed: data.allowed,
                    key: data.key,
                    tokensRequested: data.tokensRequested
                };
            } else if (response.status === 401) {
                throw new Error('Invalid API key');
            } else if (response.status === 403) {
                throw new Error('IP address not allowed');
            } else {
                throw new Error(`HTTP ${response.status}: ${response.statusText}`);
            }
        } catch (error) {
            clearTimeout(timeoutId);
            
            if (error.name === 'AbortError') {
                console.warn('Rate limiter timeout, allowing request');
            } else {
                console.warn(`Rate limiter error: ${error.message}, allowing request`);
            }
            
            // Fail open
            return {
                allowed: true,
                key,
                tokensRequested: tokens
            };
        }
    }
}
```

## TypeScript Version

```typescript
interface RateLimitRequest {
    key: string;
    tokens: number;
    apiKey?: string;
}

interface RateLimitResponse {
    key: string;
    tokensRequested: number;
    allowed: boolean;
}

interface RateLimitResult {
    allowed: boolean;
    key: string;
    tokensRequested: number;
}

class TypedRateLimiterClient {
    private baseUrl: string;
    private apiKey?: string;

    constructor(baseUrl: string = 'http://localhost:8080', apiKey?: string) {
        this.baseUrl = baseUrl.replace(/\/$/, '');
        this.apiKey = apiKey;
    }

    async checkRateLimit(key: string, tokens: number = 1): Promise<RateLimitResult> {
        const payload: RateLimitRequest = {
            key,
            tokens
        };

        if (this.apiKey) {
            payload.apiKey = this.apiKey;
        }

        try {
            const response = await fetch(`${this.baseUrl}/api/ratelimit/check`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(payload),
                signal: AbortSignal.timeout(10000)
            });

            if (response.ok || response.status === 429) {
                const data: RateLimitResponse = await response.json();
                return {
                    allowed: data.allowed,
                    key: data.key,
                    tokensRequested: data.tokensRequested
                };
            } else if (response.status === 401) {
                throw new Error('Invalid API key');
            } else if (response.status === 403) {
                throw new Error('IP address not allowed');
            } else {
                throw new Error(`HTTP ${response.status}: ${response.statusText}`);
            }
        } catch (error) {
            console.warn(`Rate limiter error: ${error}, allowing request`);
            
            return {
                allowed: true,
                key,
                tokensRequested: tokens
            };
        }
    }
}

export { TypedRateLimiterClient, RateLimitRequest, RateLimitResponse, RateLimitResult };
```

## Configuration

Environment variables:

```javascript
// config.js
module.exports = {
    rateLimiter: {
        url: process.env.RATE_LIMITER_URL || 'http://localhost:8080',
        apiKey: process.env.RATE_LIMITER_API_KEY,
        timeout: parseInt(process.env.RATE_LIMITER_TIMEOUT || '10000'),
        failOpen: process.env.RATE_LIMITER_FAIL_OPEN !== 'false'
    }
};
```

## Resilient Client with Retry

```javascript
class ResilientRateLimiterClient extends RateLimiterClient {
    constructor(baseUrl, apiKey, options = {}) {
        super(baseUrl, apiKey);
        this.maxRetries = options.maxRetries || 3;
        this.failOpen = options.failOpen !== false;
        this.retryDelay = options.retryDelay || 1000;
    }

    async checkRateLimit(key, tokens = 1) {
        let lastError;
        
        for (let attempt = 0; attempt <= this.maxRetries; attempt++) {
            try {
                const result = await super.checkRateLimit(key, tokens);
                return result;
            } catch (error) {
                lastError = error;
                
                if (attempt < this.maxRetries) {
                    const delay = this.retryDelay * Math.pow(2, attempt);
                    console.warn(`Rate limiter attempt ${attempt + 1} failed, retrying in ${delay}ms`);
                    await this.sleep(delay);
                }
            }
        }
        
        console.error(`All rate limiter attempts failed: ${lastError.message}`);
        
        if (this.failOpen) {
            console.warn('Failing open - allowing request');
            return {
                allowed: true,
                key,
                tokensRequested: tokens
            };
        } else {
            throw lastError;
        }
    }
    
    sleep(ms) {
        return new Promise(resolve => setTimeout(resolve, ms));
    }
}
```

## Usage in Next.js API Routes

```javascript
// pages/api/users/[id].js
import RateLimiterClient from '../../../lib/rate-limiter-client';

const rateLimiter = new RateLimiterClient(
    process.env.RATE_LIMITER_URL,
    process.env.RATE_LIMITER_API_KEY
);

export default async function handler(req, res) {
    const { id } = req.query;
    const clientIp = req.headers['x-forwarded-for'] || req.connection.remoteAddress;
    
    // Check rate limit
    const result = await rateLimiter.checkRateLimit(`user:${id}:${clientIp}`, 1);
    
    if (!result.allowed) {
        return res.status(429).json({
            error: 'Rate limit exceeded',
            retryAfter: 60
        });
    }
    
    // Process request
    res.json({ id, name: 'John Doe' });
}
```

## Package.json Scripts

```json
{
  "scripts": {
    "test:ratelimiter": "node test/rate-limiter-test.js",
    "dev": "RATE_LIMITER_URL=http://localhost:8080 node server.js"
  }
}
```