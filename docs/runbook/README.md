# Operations Runbook - Distributed Rate Limiter

This runbook provides step-by-step procedures for operating and troubleshooting the Distributed Rate Limiter service in production.

## Table of Contents

- [Quick Reference](#quick-reference)
- [Incident Response](#incident-response)
- [Common Issues](#common-issues)
- [Maintenance Procedures](#maintenance-procedures)
- [Monitoring and Alerting](#monitoring-and-alerting)
- [Backup and Recovery](#backup-and-recovery)
- [Scaling Operations](#scaling-operations)
- [Contact Information](#contact-information)

## Quick Reference

### Service Overview
- **Application**: Distributed Rate Limiter
- **Purpose**: Token bucket based rate limiting with Redis backend
- **Repository**: https://github.com/uppnrise/distributed-rate-limiter
- **Monitoring**: Grafana Dashboard, Prometheus Alerts

### Key Endpoints
- **Health Check**: `GET /actuator/health`
- **Metrics**: `GET /actuator/prometheus`
- **Rate Limit Check**: `POST /api/ratelimit/check`
- **Configuration**: `GET /api/ratelimit/config`

### Infrastructure Components
- **Application Pods**: 3 replicas in production
- **Redis**: Single instance with persistence
- **Load Balancer**: NGINX Ingress Controller
- **Monitoring**: Prometheus + Grafana + AlertManager

## Incident Response

### Severity Levels

#### P0 - Critical (Response: Immediate)
- Service completely down (all pods failing)
- Redis completely inaccessible
- Data corruption/loss
- Security breach

#### P1 - High (Response: 15 minutes)
- High error rate (>10% 5xx responses)
- High latency (>500ms p95)
- Partial service degradation
- Memory/CPU exhaustion

#### P2 - Medium (Response: 1 hour)
- Individual pod failures
- Redis performance issues
- Configuration problems
- Non-critical alerts

#### P3 - Low (Response: Next business day)
- Monitoring gaps
- Documentation issues
- Minor performance concerns

### Initial Response Checklist

1. **Acknowledge the incident** in your incident management system
2. **Check service status** using monitoring dashboards
3. **Verify external dependencies** (Redis, network connectivity)
4. **Review recent changes** (deployments, configuration updates)
5. **Gather initial data** (logs, metrics, pod status)
6. **Escalate if needed** based on severity

## Common Issues

### Rate Limiter Down

**Symptoms**: 
- Health check failing
- No response from service endpoints
- Prometheus target down

**Investigation Steps**:
```bash
# Check pod status
kubectl get pods -n rate-limiter -l app.kubernetes.io/name=distributed-rate-limiter

# Check pod logs
kubectl logs -n rate-limiter -l app.kubernetes.io/name=distributed-rate-limiter --tail=100

# Check service and ingress
kubectl get svc,ingress -n rate-limiter

# Check recent events
kubectl get events -n rate-limiter --sort-by=.metadata.creationTimestamp
```

**Common Causes & Solutions**:
- **Redis connectivity**: Check Redis pod status and network policies
- **Resource exhaustion**: Check CPU/memory limits and usage
- **Configuration issues**: Verify ConfigMap and Secret values
- **Image pull issues**: Check image availability and credentials

**Recovery Actions**:
```bash
# Restart deployment
kubectl rollout restart deployment/rate-limiter -n rate-limiter

# Scale up if needed
kubectl scale deployment/rate-limiter -n rate-limiter --replicas=5

# Emergency rollback
kubectl rollout undo deployment/rate-limiter -n rate-limiter
```

### High Latency

**Symptoms**:
- Response times >500ms (95th percentile)
- Slow API responses
- Client timeouts

**Investigation Steps**:
```bash
# Check application metrics
curl http://rate-limiter-service/actuator/metrics/http.server.requests

# Check Redis latency
kubectl exec -n rate-limiter redis-pod -- redis-cli --latency-history

# Check resource usage
kubectl top pods -n rate-limiter
```

**Common Causes & Solutions**:
- **Redis performance**: Check Redis CPU/memory, connection pool
- **High load**: Scale application pods, check rate limiting thresholds
- **Network issues**: Check ingress controller performance
- **GC pressure**: Adjust JVM settings, increase memory limits

**Mitigation**:
```bash
# Increase replicas
kubectl scale deployment/rate-limiter -n rate-limiter --replicas=6

# Increase resources temporarily
kubectl patch deployment rate-limiter -n rate-limiter -p '{"spec":{"template":{"spec":{"containers":[{"name":"rate-limiter","resources":{"limits":{"memory":"2Gi","cpu":"2000m"}}}]}}}}'
```

### High Error Rate

**Symptoms**:
- >10% HTTP 5xx responses
- Application errors in logs
- Failed health checks

**Investigation Steps**:
```bash
# Check error logs
kubectl logs -n rate-limiter -l app.kubernetes.io/name=distributed-rate-limiter | grep ERROR

# Check Redis connectivity
kubectl exec -n rate-limiter redis-pod -- redis-cli ping

# Check rate limiting violations
kubectl logs -n rate-limiter -l app.kubernetes.io/name=distributed-rate-limiter | grep "Rate limit VIOLATED"
```

**Common Causes & Solutions**:
- **Redis connection failures**: Check Redis pod, network policies, credentials
- **Application bugs**: Review recent deployments, check exception logs
- **Resource constraints**: Check memory/CPU limits
- **Configuration errors**: Verify application properties

### Redis Down

**Symptoms**:
- Redis health check failing
- Rate limiter unable to connect to Redis
- All rate limiting failing open or closed

**Investigation Steps**:
```bash
# Check Redis pod
kubectl get pods -n rate-limiter -l app.kubernetes.io/name=redis

# Check Redis logs
kubectl logs -n rate-limiter redis-pod

# Check persistent volume
kubectl get pv,pvc -n rate-limiter

# Test Redis connectivity
kubectl exec -n rate-limiter redis-pod -- redis-cli ping
```

**Recovery Actions**:
```bash
# Restart Redis pod
kubectl delete pod -n rate-limiter redis-pod

# Check data persistence
kubectl exec -n rate-limiter redis-pod -- redis-cli DBSIZE

# If data is corrupted, restore from backup
./scripts/backup/redis-recovery.sh -f latest_backup.rdb -n rate-limiter
```

### Redis High Memory

**Symptoms**:
- Redis memory usage >90%
- Redis evicting keys
- Performance degradation

**Investigation Steps**:
```bash
# Check Redis memory usage
kubectl exec -n rate-limiter redis-pod -- redis-cli INFO memory

# Check key count and types
kubectl exec -n rate-limiter redis-pod -- redis-cli DBSIZE
kubectl exec -n rate-limiter redis-pod -- redis-cli --scan | head -20
```

**Solutions**:
```bash
# Increase Redis memory limit
kubectl patch deployment redis -n rate-limiter -p '{"spec":{"template":{"spec":{"containers":[{"name":"redis","resources":{"limits":{"memory":"2Gi"}}}]}}}}'

# Manual cleanup if needed
kubectl exec -n rate-limiter redis-pod -- redis-cli FLUSHDB

# Adjust cleanup interval
kubectl patch configmap rate-limiter-config -n rate-limiter --patch '{"data":{"application.properties":"ratelimiter.cleanupIntervalMs=30000"}}'
```

### Suspicious Activity

**Symptoms**:
- Extremely high rate limit violations (>1000/sec)
- Unusual traffic patterns
- Potential DDoS attack

**Investigation Steps**:
```bash
# Check top rate limited keys
kubectl logs -n rate-limiter -l app.kubernetes.io/name=distributed-rate-limiter | grep "VIOLATED" | awk '{print $NF}' | sort | uniq -c | sort -nr | head -10

# Check ingress logs for source IPs
kubectl logs -n ingress-nginx -l app.kubernetes.io/name=ingress-nginx | grep rate-limiter

# Review request patterns
kubectl exec -n rate-limiter rate-limiter-pod -- curl http://localhost:8080/actuator/metrics/rate.limiter.requests
```

**Response Actions**:
```bash
# Increase ingress rate limits temporarily
kubectl patch ingress rate-limiter-ingress -n rate-limiter --patch '{"metadata":{"annotations":{"nginx.ingress.kubernetes.io/rate-limit-rpm":"500"}}}'

# Scale up application
kubectl scale deployment/rate-limiter -n rate-limiter --replicas=10

# Contact security team if DDoS suspected
```

## Maintenance Procedures

### Rolling Updates

```bash
# Update to new version
./scripts/deployment/deploy.sh prod IMAGE_TAG=v1.2.3

# Monitor rollout
kubectl rollout status deployment/rate-limiter -n rate-limiter

# Rollback if issues
kubectl rollout undo deployment/rate-limiter -n rate-limiter
```

### Configuration Updates

```bash
# Update ConfigMap
kubectl edit configmap rate-limiter-config -n rate-limiter

# Restart deployment to pick up changes
kubectl rollout restart deployment/rate-limiter -n rate-limiter
```

### Scaling Operations

```bash
# Scale up for high traffic
kubectl scale deployment/rate-limiter -n rate-limiter --replicas=6

# Enable HPA for automatic scaling
kubectl autoscale deployment/rate-limiter -n rate-limiter --cpu-percent=70 --min=3 --max=10
```

## Backup and Recovery

### Regular Backup

```bash
# Create backup
./scripts/backup/redis-backup.sh

# Verify backup
ls -la /backups/redis/
```

### Recovery Procedures

```bash
# List available backups
ls -la /backups/redis/

# Restore from backup
./scripts/backup/redis-recovery.sh -f redis_backup_20231201_120000.rdb -n rate-limiter

# Verify recovery
kubectl exec -n rate-limiter redis-pod -- redis-cli DBSIZE
```

## Contact Information

### Escalation Path
1. **On-call Engineer**: [Primary contact]
2. **Team Lead**: [Secondary contact]
3. **Platform Team**: [Infrastructure support]
4. **Security Team**: [For security incidents]

### Communication Channels
- **Incident Channel**: #incidents-rate-limiter
- **Team Channel**: #team-platform
- **Status Page**: https://status.company.com

### External Dependencies
- **Redis Provider**: [Contact information]
- **Infrastructure Team**: [Contact information]
- **Network Team**: [Contact information]

---

## Emergency Contacts

**Critical P0 Issues**: Call the on-call rotation immediately
**Security Issues**: Contact security team immediately
**Infrastructure Issues**: Page the platform team

Last Updated: [Date]
Next Review: [Date + 3 months]