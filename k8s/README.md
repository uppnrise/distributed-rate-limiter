# Production Deployment Infrastructure

This directory contains comprehensive production-ready deployment infrastructure for the Distributed Rate Limiter service.

## Directory Structure

```
k8s/
├── base/                          # Base Kubernetes manifests
│   ├── namespace.yaml             # Namespace, ResourceQuota, LimitRange
│   ├── configmap.yaml             # Application configuration
│   ├── secrets.yaml               # Secret templates
│   ├── rbac.yaml                  # ServiceAccount, Role, RoleBinding
│   ├── redis.yaml                 # Redis deployment with monitoring
│   ├── deployment.yaml            # Rate limiter deployment + service + PDB
│   ├── ingress.yaml              # Ingress configuration with security
│   └── backup-cronjob.yaml       # Automated backup CronJob
├── environments/                  # Environment-specific configurations
│   ├── dev/                      # Development environment
│   │   ├── kustomization.yaml    # Dev overlay configuration
│   │   ├── namespace-patch.yaml  # Dev namespace overrides
│   │   ├── deployment-patch.yaml # Dev resource limits
│   │   ├── configmap-patch.yaml  # Dev configuration overrides
│   │   └── ingress-patch.yaml    # Dev ingress settings
│   └── prod/                     # Production environment
│       ├── kustomization.yaml    # Production overlay
│       ├── deployment-patch.yaml # Production optimizations
│       ├── hpa.yaml              # Horizontal Pod Autoscaler
│       └── network-policy.yaml  # Network security policies
└── monitoring/                   # Monitoring and observability
    ├── prometheus/
    │   ├── config.yaml           # Prometheus configuration
    │   └── rules.yaml            # Alert rules
    └── grafana/
        └── dashboard.json        # Grafana dashboard definition

scripts/
├── backup/
│   ├── redis-backup.sh          # Redis backup automation
│   └── redis-recovery.sh        # Redis recovery procedures
└── deployment/
    └── deploy.sh                # Deployment automation script

docs/
└── runbook/
    └── README.md                # Operations runbook
```

## Quick Start

### Prerequisites

1. **Kubernetes cluster** with RBAC enabled
2. **kubectl** configured with cluster access
3. **kustomize** installed (v3.0+)
4. **Docker registry** access for pulling images

### Deploy to Development

```bash
# Deploy to development environment
./scripts/deployment/deploy.sh dev

# Check deployment status
kubectl get all -n rate-limiter-dev
```

### Deploy to Production

```bash
# Deploy to production with specific image tag
./scripts/deployment/deploy.sh prod IMAGE_TAG=v1.0.0

# Verify deployment
kubectl get pods -n rate-limiter
kubectl get ingress -n rate-limiter
```

## Environment Configurations

### Development Environment
- **Namespace**: `rate-limiter-dev`
- **Replicas**: 1 instance
- **Resources**: Minimal (256Mi RAM, 100m CPU)
- **Configuration**: Debug logging, permissive rate limits
- **Ingress**: HTTP allowed, no TLS required

### Production Environment  
- **Namespace**: `rate-limiter`
- **Replicas**: 3 instances (auto-scaling 3-10)
- **Resources**: Production-ready (1Gi RAM, 1 CPU limit)
- **Configuration**: Optimized for performance and security
- **Ingress**: HTTPS only, rate limiting, security headers
- **Additional**: Network policies, PodDisruptionBudget, HPA

## Key Features

### Security
- **Non-root containers** with security contexts
- **Read-only root filesystems** where possible
- **Network policies** for traffic control (production)
- **RBAC** with minimal required permissions
- **Secret management** with environment-specific overrides

### High Availability
- **Multiple replicas** with anti-affinity rules
- **Health checks** (liveness, readiness, startup probes)
- **Graceful shutdown** with proper termination handling
- **Pod Disruption Budget** to maintain availability
- **Horizontal Pod Autoscaler** for automatic scaling

### Monitoring & Observability
- **Prometheus metrics** collection and scraping
- **Redis monitoring** with dedicated exporter
- **Alert rules** for critical issues
- **Grafana dashboard** for visualization
- **Structured logging** with JSON format

### Backup & Recovery
- **Automated daily backups** via CronJob
- **S3 integration** for offsite storage
- **Recovery scripts** with validation
- **Retention management** (30 days default)

### Configuration Management
- **Environment-specific** configuration overrides
- **Secret management** with base64 encoding
- **ConfigMap** for application properties
- **Kustomize** for clean environment separation

## Deployment Process

### Manual Deployment

```bash
# 1. Build and validate manifests
kustomize build k8s/environments/prod > manifests.yaml
kubectl apply --dry-run=server -f manifests.yaml

# 2. Apply to cluster
kubectl apply -f manifests.yaml

# 3. Wait for rollout
kubectl rollout status deployment/rate-limiter -n rate-limiter

# 4. Verify health
kubectl exec -n rate-limiter deployment/rate-limiter -- curl http://localhost:8080/actuator/health
```

### Automated Deployment

```bash
# Use the deployment script
./scripts/deployment/deploy.sh prod IMAGE_TAG=v1.2.3

# Or with dry-run
DRY_RUN=true ./scripts/deployment/deploy.sh prod
```

## Monitoring Setup

### Prometheus Configuration
```bash
# Apply Prometheus configuration
kubectl apply -f k8s/monitoring/prometheus/

# Verify metrics collection
kubectl port-forward -n monitoring svc/prometheus 9090:9090
# Open http://localhost:9090
```

### Grafana Dashboard
```bash
# Import dashboard to Grafana
# Use the JSON from k8s/monitoring/grafana/dashboard.json
```

## Backup Operations

### Manual Backup
```bash
# Create immediate backup
./scripts/backup/redis-backup.sh

# Backup with S3 upload
S3_BUCKET=my-backup-bucket ./scripts/backup/redis-backup.sh
```

### Recovery from Backup
```bash
# List available backups
ls -la /backups/redis/

# Restore from backup
./scripts/backup/redis-recovery.sh -f redis_backup_20231201_120000.rdb -n rate-limiter

# Restore with confirmation prompt
./scripts/backup/redis-recovery.sh -f backup.rdb --force
```

## Troubleshooting

### Common Issues

1. **Image Pull Errors**
   ```bash
   # Check image availability
   docker pull ghcr.io/uppnrise/distributed-rate-limiter:latest
   
   # Update image pull secrets if needed
   kubectl create secret docker-registry regcred \
     --docker-server=ghcr.io \
     --docker-username=username \
     --docker-password=token
   ```

2. **Resource Constraints**
   ```bash
   # Check resource usage
   kubectl top pods -n rate-limiter
   kubectl describe nodes
   
   # Adjust resource limits
   kubectl patch deployment rate-limiter -n rate-limiter -p '{"spec":{"template":{"spec":{"containers":[{"name":"rate-limiter","resources":{"limits":{"memory":"2Gi"}}}]}}}}'
   ```

3. **Redis Connectivity**
   ```bash
   # Test Redis connection
   kubectl exec -n rate-limiter deployment/redis -- redis-cli ping
   
   # Check Redis logs
   kubectl logs -n rate-limiter deployment/redis -c redis
   ```

### Health Checks

```bash
# Application health
kubectl exec -n rate-limiter deployment/rate-limiter -- curl http://localhost:8080/actuator/health

# Redis health  
kubectl exec -n rate-limiter deployment/redis -- redis-cli ping

# Check all services
kubectl get pods,svc,ingress -n rate-limiter
```

## Security Considerations

### Secrets Management
- Never commit actual secrets to version control
- Use Kubernetes secrets or external secret managers
- Rotate secrets regularly
- Limit secret access with RBAC

### Network Security
- Network policies are applied in production
- Ingress controller provides TLS termination
- Internal traffic is encrypted where possible
- Rate limiting at multiple layers

### Container Security
- Containers run as non-root users
- Read-only root filesystems
- Minimal container images
- Regular security updates

## Scaling Operations

### Manual Scaling
```bash
# Scale application pods
kubectl scale deployment/rate-limiter -n rate-limiter --replicas=5

# Check scaling status
kubectl get deployment/rate-limiter -n rate-limiter
```

### Auto-scaling
```bash
# Horizontal Pod Autoscaler is configured in production
kubectl get hpa -n rate-limiter

# Adjust HPA settings
kubectl patch hpa rate-limiter-hpa -n rate-limiter -p '{"spec":{"maxReplicas":15}}'
```

## Maintenance

### Rolling Updates
```bash
# Update application image
kubectl set image deployment/rate-limiter rate-limiter=ghcr.io/uppnrise/distributed-rate-limiter:v1.2.3 -n rate-limiter

# Monitor rollout
kubectl rollout status deployment/rate-limiter -n rate-limiter

# Rollback if needed
kubectl rollout undo deployment/rate-limiter -n rate-limiter
```

### Configuration Updates
```bash
# Update ConfigMap
kubectl edit configmap rate-limiter-config -n rate-limiter

# Restart deployment to pick up changes
kubectl rollout restart deployment/rate-limiter -n rate-limiter
```

## Support

- **Documentation**: See `docs/runbook/README.md` for operational procedures
- **Monitoring**: Grafana dashboards and Prometheus alerts
- **Logs**: Centralized logging with structured JSON format
- **Metrics**: Comprehensive application and infrastructure metrics

---

**Last Updated**: December 2023
**Maintainer**: Platform Team
**Review Cycle**: Quarterly