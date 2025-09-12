# Go Client Example

This document provides a Go client implementation for the Distributed Rate Limiter API.

## Simple HTTP Client

### Basic Rate Limit Check

```go
package main

import (
    "bytes"
    "encoding/json"
    "fmt"
    "io"
    "net/http"
    "time"
)

// RateLimitRequest represents the request payload
type RateLimitRequest struct {
    Key    string `json:"key"`
    Tokens int    `json:"tokens"`
    APIKey string `json:"apiKey,omitempty"`
}

// RateLimitResponse represents the response payload
type RateLimitResponse struct {
    Key             string `json:"key"`
    TokensRequested int    `json:"tokensRequested"`
    Allowed         bool   `json:"allowed"`
}

// RateLimiterClient is a simple HTTP client for the rate limiter
type RateLimiterClient struct {
    BaseURL    string
    HTTPClient *http.Client
    APIKey     string
}

// NewRateLimiterClient creates a new rate limiter client
func NewRateLimiterClient(baseURL, apiKey string) *RateLimiterClient {
    return &RateLimiterClient{
        BaseURL: baseURL,
        HTTPClient: &http.Client{
            Timeout: 10 * time.Second,
        },
        APIKey: apiKey,
    }
}

// CheckRateLimit checks if a request is allowed for the given key
func (c *RateLimiterClient) CheckRateLimit(key string, tokens int) (*RateLimitResponse, error) {
    request := RateLimitRequest{
        Key:    key,
        Tokens: tokens,
        APIKey: c.APIKey,
    }

    jsonData, err := json.Marshal(request)
    if err != nil {
        return nil, fmt.Errorf("failed to marshal request: %w", err)
    }

    req, err := http.NewRequest("POST", c.BaseURL+"/api/ratelimit/check", bytes.NewBuffer(jsonData))
    if err != nil {
        return nil, fmt.Errorf("failed to create request: %w", err)
    }

    req.Header.Set("Content-Type", "application/json")

    resp, err := c.HTTPClient.Do(req)
    if err != nil {
        return nil, fmt.Errorf("failed to send request: %w", err)
    }
    defer resp.Body.Close()

    body, err := io.ReadAll(resp.Body)
    if err != nil {
        return nil, fmt.Errorf("failed to read response: %w", err)
    }

    var response RateLimitResponse
    if err := json.Unmarshal(body, &response); err != nil {
        return nil, fmt.Errorf("failed to unmarshal response: %w", err)
    }

    // Check HTTP status
    switch resp.StatusCode {
    case http.StatusOK:
        return &response, nil
    case http.StatusTooManyRequests:
        return &response, fmt.Errorf("rate limit exceeded")
    case http.StatusUnauthorized:
        return &response, fmt.Errorf("invalid API key")
    case http.StatusForbidden:
        return &response, fmt.Errorf("IP address not allowed")
    default:
        return &response, fmt.Errorf("unexpected status code: %d", resp.StatusCode)
    }
}

func main() {
    client := NewRateLimiterClient("http://localhost:8080", "your-api-key")

    // Test rate limiting
    for i := 0; i < 15; i++ {
        response, err := client.CheckRateLimit("user:123", 1)
        if err != nil {
            fmt.Printf("Request %d: Error - %v\n", i+1, err)
        } else {
            fmt.Printf("Request %d: Allowed = %t\n", i+1, response.Allowed)
        }

        // Small delay between requests
        time.Sleep(100 * time.Millisecond)
    }
}
```

## Advanced Client with Middleware

### Gin Middleware Implementation

```go
package middleware

import (
    "bytes"
    "encoding/json"
    "fmt"
    "net/http"
    "time"

    "github.com/gin-gonic/gin"
)

// RateLimiterMiddleware provides rate limiting for Gin
type RateLimiterMiddleware struct {
    client      *RateLimiterClient
    keyFunc     func(c *gin.Context) string
    tokensFunc  func(c *gin.Context) int
    onExceeded  func(c *gin.Context)
}

// NewRateLimiterMiddleware creates a new rate limiter middleware
func NewRateLimiterMiddleware(baseURL, apiKey string) *RateLimiterMiddleware {
    return &RateLimiterMiddleware{
        client: NewRateLimiterClient(baseURL, apiKey),
        keyFunc: func(c *gin.Context) string {
            // Default: use client IP
            return "ip:" + c.ClientIP()
        },
        tokensFunc: func(c *gin.Context) int {
            // Default: 1 token per request
            return 1
        },
        onExceeded: func(c *gin.Context) {
            c.JSON(http.StatusTooManyRequests, gin.H{
                "error": "Rate limit exceeded",
            })
            c.Abort()
        },
    }
}

// WithKeyFunc sets a custom key generation function
func (m *RateLimiterMiddleware) WithKeyFunc(fn func(c *gin.Context) string) *RateLimiterMiddleware {
    m.keyFunc = fn
    return m
}

// WithTokensFunc sets a custom tokens calculation function
func (m *RateLimiterMiddleware) WithTokensFunc(fn func(c *gin.Context) int) *RateLimiterMiddleware {
    m.tokensFunc = fn
    return m
}

// WithOnExceeded sets a custom handler for when rate limit is exceeded
func (m *RateLimiterMiddleware) WithOnExceeded(fn func(c *gin.Context)) *RateLimiterMiddleware {
    m.onExceeded = fn
    return m
}

// Handler returns the Gin middleware handler
func (m *RateLimiterMiddleware) Handler() gin.HandlerFunc {
    return func(c *gin.Context) {
        key := m.keyFunc(c)
        tokens := m.tokensFunc(c)

        response, err := m.client.CheckRateLimit(key, tokens)
        if err != nil {
            // On error, allow the request to proceed (fail open)
            c.Next()
            return
        }

        if !response.Allowed {
            m.onExceeded(c)
            return
        }

        c.Next()
    }
}

// Example usage
func setupRouter() *gin.Engine {
    r := gin.Default()

    // Create rate limiter middleware
    rateLimiter := NewRateLimiterMiddleware("http://localhost:8080", "your-api-key").
        WithKeyFunc(func(c *gin.Context) string {
            // Use user ID if authenticated, otherwise IP
            if userID := c.GetHeader("X-User-ID"); userID != "" {
                return "user:" + userID
            }
            return "ip:" + c.ClientIP()
        }).
        WithTokensFunc(func(c *gin.Context) int {
            // Different token costs for different endpoints
            switch c.Request.URL.Path {
            case "/api/expensive":
                return 5
            case "/api/upload":
                return 3
            default:
                return 1
            }
        })

    // Apply to all routes
    r.Use(rateLimiter.Handler())

    r.GET("/api/users", func(c *gin.Context) {
        c.JSON(200, gin.H{"message": "Users list"})
    })

    r.POST("/api/expensive", func(c *gin.Context) {
        c.JSON(200, gin.H{"message": "Expensive operation completed"})
    })

    return r
}
```

## Configuration Management Client

```go
package main

import (
    "bytes"
    "encoding/json"
    "fmt"
    "io"
    "net/http"
)

// ConfigRequest represents configuration update request
type ConfigRequest struct {
    Capacity         *int `json:"capacity,omitempty"`
    RefillRate       *int `json:"refillRate,omitempty"`
    CleanupIntervalMs *int64 `json:"cleanupIntervalMs,omitempty"`
}

// ConfigResponse represents current configuration
type ConfigResponse struct {
    Capacity          int                    `json:"capacity"`
    RefillRate        int                    `json:"refillRate"`
    CleanupIntervalMs int64                  `json:"cleanupIntervalMs"`
    Keys              map[string]interface{} `json:"keys"`
    Patterns          map[string]interface{} `json:"patterns"`
}

// ConfigurationClient manages rate limiter configuration
type ConfigurationClient struct {
    client *RateLimiterClient
}

// NewConfigurationClient creates a new configuration client
func NewConfigurationClient(baseURL, apiKey string) *ConfigurationClient {
    return &ConfigurationClient{
        client: NewRateLimiterClient(baseURL, apiKey),
    }
}

// GetConfiguration retrieves current configuration
func (c *ConfigurationClient) GetConfiguration() (*ConfigResponse, error) {
    req, err := http.NewRequest("GET", c.client.BaseURL+"/api/ratelimit/config", nil)
    if err != nil {
        return nil, fmt.Errorf("failed to create request: %w", err)
    }

    resp, err := c.client.HTTPClient.Do(req)
    if err != nil {
        return nil, fmt.Errorf("failed to send request: %w", err)
    }
    defer resp.Body.Close()

    if resp.StatusCode != http.StatusOK {
        return nil, fmt.Errorf("unexpected status code: %d", resp.StatusCode)
    }

    body, err := io.ReadAll(resp.Body)
    if err != nil {
        return nil, fmt.Errorf("failed to read response: %w", err)
    }

    var config ConfigResponse
    if err := json.Unmarshal(body, &config); err != nil {
        return nil, fmt.Errorf("failed to unmarshal response: %w", err)
    }

    return &config, nil
}

// UpdateDefaultConfiguration updates default rate limiting configuration
func (c *ConfigurationClient) UpdateDefaultConfiguration(config ConfigRequest) error {
    jsonData, err := json.Marshal(config)
    if err != nil {
        return fmt.Errorf("failed to marshal request: %w", err)
    }

    req, err := http.NewRequest("POST", c.client.BaseURL+"/api/ratelimit/config/default", bytes.NewBuffer(jsonData))
    if err != nil {
        return fmt.Errorf("failed to create request: %w", err)
    }

    req.Header.Set("Content-Type", "application/json")

    resp, err := c.client.HTTPClient.Do(req)
    if err != nil {
        return fmt.Errorf("failed to send request: %w", err)
    }
    defer resp.Body.Close()

    if resp.StatusCode != http.StatusOK {
        return fmt.Errorf("unexpected status code: %d", resp.StatusCode)
    }

    return nil
}

// UpdateKeyConfiguration sets configuration for a specific key
func (c *ConfigurationClient) UpdateKeyConfiguration(key string, config ConfigRequest) error {
    jsonData, err := json.Marshal(config)
    if err != nil {
        return fmt.Errorf("failed to marshal request: %w", err)
    }

    url := fmt.Sprintf("%s/api/ratelimit/config/keys/%s", c.client.BaseURL, key)
    req, err := http.NewRequest("POST", url, bytes.NewBuffer(jsonData))
    if err != nil {
        return fmt.Errorf("failed to create request: %w", err)
    }

    req.Header.Set("Content-Type", "application/json")

    resp, err := c.client.HTTPClient.Do(req)
    if err != nil {
        return fmt.Errorf("failed to send request: %w", err)
    }
    defer resp.Body.Close()

    if resp.StatusCode != http.StatusOK {
        return fmt.Errorf("unexpected status code: %d", resp.StatusCode)
    }

    return nil
}

func main() {
    configClient := NewConfigurationClient("http://localhost:8080", "your-api-key")

    // Get current configuration
    config, err := configClient.GetConfiguration()
    if err != nil {
        fmt.Printf("Error getting configuration: %v\n", err)
        return
    }

    fmt.Printf("Current configuration: %+v\n", config)

    // Update default configuration
    newConfig := ConfigRequest{
        Capacity:   &[]int{20}[0],
        RefillRate: &[]int{5}[0],
    }

    if err := configClient.UpdateDefaultConfiguration(newConfig); err != nil {
        fmt.Printf("Error updating configuration: %v\n", err)
        return
    }

    fmt.Println("Configuration updated successfully")

    // Set configuration for premium users
    premiumConfig := ConfigRequest{
        Capacity:   &[]int{100}[0],
        RefillRate: &[]int{25}[0],
    }

    if err := configClient.UpdateKeyConfiguration("premium_user", premiumConfig); err != nil {
        fmt.Printf("Error updating key configuration: %v\n", err)
        return
    }

    fmt.Println("Premium user configuration updated successfully")
}
```

## Error Handling and Resilience

```go
package ratelimiter

import (
    "context"
    "fmt"
    "time"
)

// ResilienceOptions configures resilience features
type ResilienceOptions struct {
    MaxRetries      int
    RetryDelay      time.Duration
    CircuitBreaker  bool
    FallbackAllowed bool
}

// ResilientClient wraps the basic client with resilience features
type ResilientClient struct {
    client  *RateLimiterClient
    options ResilienceOptions
}

// NewResilientClient creates a new resilient rate limiter client
func NewResilientClient(baseURL, apiKey string, options ResilienceOptions) *ResilientClient {
    return &ResilientClient{
        client:  NewRateLimiterClient(baseURL, apiKey),
        options: options,
    }
}

// CheckRateLimitWithRetry checks rate limit with retry logic
func (r *ResilientClient) CheckRateLimitWithRetry(ctx context.Context, key string, tokens int) (*RateLimitResponse, error) {
    var lastErr error

    for attempt := 0; attempt <= r.options.MaxRetries; attempt++ {
        if attempt > 0 {
            select {
            case <-ctx.Done():
                return nil, ctx.Err()
            case <-time.After(r.options.RetryDelay):
                // Continue with retry
            }
        }

        response, err := r.client.CheckRateLimit(key, tokens)
        if err == nil {
            return response, nil
        }

        lastErr = err

        // Don't retry on certain errors
        if isNonRetryableError(err) {
            break
        }
    }

    if r.options.FallbackAllowed {
        // Return allowed response when service is unavailable
        return &RateLimitResponse{
            Key:             key,
            TokensRequested: tokens,
            Allowed:         true,
        }, nil
    }

    return nil, fmt.Errorf("rate limit check failed after %d attempts: %w", r.options.MaxRetries+1, lastErr)
}

func isNonRetryableError(err error) bool {
    // Don't retry on authentication or rate limit exceeded errors
    errStr := err.Error()
    return errStr == "invalid API key" || errStr == "rate limit exceeded" || errStr == "IP address not allowed"
}

// Example usage with context and timeout
func exampleUsage() {
    client := NewResilientClient("http://localhost:8080", "your-api-key", ResilienceOptions{
        MaxRetries:      3,
        RetryDelay:      100 * time.Millisecond,
        FallbackAllowed: true,
    })

    ctx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
    defer cancel()

    response, err := client.CheckRateLimitWithRetry(ctx, "user:123", 1)
    if err != nil {
        fmt.Printf("Rate limit check failed: %v\n", err)
        return
    }

    fmt.Printf("Rate limit check result: allowed=%t\n", response.Allowed)
}
```

## Testing

```go
package ratelimiter_test

import (
    "net/http"
    "net/http/httptest"
    "testing"
    "time"
)

func TestRateLimiterClient(t *testing.T) {
    // Create mock server
    server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
        if r.URL.Path == "/api/ratelimit/check" {
            w.Header().Set("Content-Type", "application/json")
            w.WriteHeader(http.StatusOK)
            w.Write([]byte(`{"key":"test","tokensRequested":1,"allowed":true}`))
        }
    }))
    defer server.Close()

    client := NewRateLimiterClient(server.URL, "test-key")

    response, err := client.CheckRateLimit("test", 1)
    if err != nil {
        t.Fatalf("Expected no error, got %v", err)
    }

    if !response.Allowed {
        t.Fatalf("Expected allowed=true, got %v", response.Allowed)
    }

    if response.Key != "test" {
        t.Fatalf("Expected key=test, got %v", response.Key)
    }
}

func TestRateLimitExceeded(t *testing.T) {
    server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
        w.Header().Set("Content-Type", "application/json")
        w.WriteHeader(http.StatusTooManyRequests)
        w.Write([]byte(`{"key":"test","tokensRequested":1,"allowed":false}`))
    }))
    defer server.Close()

    client := NewRateLimiterClient(server.URL, "test-key")

    _, err := client.CheckRateLimit("test", 1)
    if err == nil {
        t.Fatal("Expected error for rate limit exceeded")
    }

    if err.Error() != "rate limit exceeded" {
        t.Fatalf("Expected 'rate limit exceeded' error, got %v", err)
    }
}
```

## Integration Example

```go
// main.go - Complete example application
package main

import (
    "log"
    "net/http"

    "github.com/gin-gonic/gin"
)

func main() {
    r := gin.Default()

    // Initialize rate limiter middleware
    rateLimiter := NewRateLimiterMiddleware("http://localhost:8080", "").
        WithKeyFunc(func(c *gin.Context) string {
            // Use API key or IP for rate limiting
            if apiKey := c.GetHeader("X-API-Key"); apiKey != "" {
                return "api:" + apiKey
            }
            return "ip:" + c.ClientIP()
        }).
        WithTokensFunc(func(c *gin.Context) int {
            // Different costs for different operations
            switch c.Request.Method {
            case "POST", "PUT", "DELETE":
                return 3
            default:
                return 1
            }
        })

    // Apply rate limiting to API routes
    api := r.Group("/api")
    api.Use(rateLimiter.Handler())

    api.GET("/users", func(c *gin.Context) {
        c.JSON(200, gin.H{"users": []string{"alice", "bob", "charlie"}})
    })

    api.POST("/users", func(c *gin.Context) {
        c.JSON(201, gin.H{"message": "User created"})
    })

    log.Println("Server starting on :8081")
    log.Fatal(http.ListenAndServe(":8081", r))
}
```

This Go client provides:

- **Simple HTTP client** for basic rate limit checks
- **Middleware integration** for Gin web framework
- **Configuration management** for dynamic rate limit updates
- **Resilience features** including retries and fallback behavior
- **Comprehensive testing** examples
- **Production-ready patterns** with proper error handling

To use this client in your project:

1. Install dependencies: `go mod init your-project && go get github.com/gin-gonic/gin`
2. Copy the relevant client code for your use case
3. Update the base URL and API key for your environment
4. Integrate with your application's authentication and routing logic