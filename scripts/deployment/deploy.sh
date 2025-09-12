#!/bin/bash
set -euo pipefail

# Deployment Script for Distributed Rate Limiter
# This script deploys the rate limiter to different environments

# Configuration
ENVIRONMENT="${1:-dev}"
NAMESPACE=""
KUSTOMIZE_PATH=""
IMAGE_TAG="${IMAGE_TAG:-latest}"
DRY_RUN="${DRY_RUN:-false}"
WAIT_TIMEOUT="${WAIT_TIMEOUT:-600s}"

# Logging
log() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $*" >&2
}

# Error handling
error_exit() {
    log "ERROR: $1"
    exit 1
}

# Show usage
usage() {
    cat << EOF
Usage: $0 ENVIRONMENT [OPTIONS]

Deploy Distributed Rate Limiter to Kubernetes

ENVIRONMENTS:
    dev         Development environment
    staging     Staging environment  
    prod        Production environment

OPTIONS:
    IMAGE_TAG=tag       Docker image tag to deploy (default: latest)
    DRY_RUN=true        Perform dry run without applying changes
    WAIT_TIMEOUT=600s   Timeout for waiting for deployment to be ready

EXAMPLES:
    $0 dev
    $0 prod IMAGE_TAG=v1.2.3
    DRY_RUN=true $0 staging
    $0 prod IMAGE_TAG=v1.0.0 WAIT_TIMEOUT=900s

EOF
    exit 1
}

# Validate environment
validate_environment() {
    case "$ENVIRONMENT" in
        dev)
            NAMESPACE="rate-limiter-dev"
            KUSTOMIZE_PATH="k8s/environments/dev"
            ;;
        staging)
            NAMESPACE="rate-limiter-staging"
            KUSTOMIZE_PATH="k8s/environments/staging"
            ;;
        prod)
            NAMESPACE="rate-limiter"
            KUSTOMIZE_PATH="k8s/environments/prod"
            ;;
        *)
            error_exit "Invalid environment: $ENVIRONMENT. Use 'dev', 'staging', or 'prod'"
            ;;
    esac
    
    log "Deploying to environment: $ENVIRONMENT"
    log "Namespace: $NAMESPACE"
    log "Kustomize path: $KUSTOMIZE_PATH"
}

# Check prerequisites
check_prerequisites() {
    log "Checking prerequisites..."
    
    # Check kubectl
    if ! command -v kubectl &> /dev/null; then
        error_exit "kubectl is not installed or not in PATH"
    fi
    
    # Check kustomize
    if ! command -v kustomize &> /dev/null; then
        error_exit "kustomize is not installed or not in PATH"
    fi
    
    # Check kubectl context
    local current_context=$(kubectl config current-context)
    log "Current kubectl context: $current_context"
    
    # Validate kustomize path exists
    if [[ ! -d "$KUSTOMIZE_PATH" ]]; then
        error_exit "Kustomize path does not exist: $KUSTOMIZE_PATH"
    fi
    
    log "Prerequisites check passed"
}

# Validate manifests
validate_manifests() {
    log "Validating Kubernetes manifests..."
    
    # Build with kustomize and validate
    local temp_manifest="/tmp/rate-limiter-manifest-$ENVIRONMENT.yaml"
    kustomize build "$KUSTOMIZE_PATH" > "$temp_manifest" || error_exit "Failed to build kustomize manifests"
    
    # Dry run validation
    kubectl apply --dry-run=client -f "$temp_manifest" > /dev/null || error_exit "Manifest validation failed"
    
    log "Manifests validation passed"
    rm -f "$temp_manifest"
}

# Create namespace if needed
create_namespace() {
    if ! kubectl get namespace "$NAMESPACE" &> /dev/null; then
        log "Creating namespace: $NAMESPACE"
        if [[ "$DRY_RUN" == "true" ]]; then
            log "DRY RUN: Would create namespace $NAMESPACE"
        else
            kubectl create namespace "$NAMESPACE" || error_exit "Failed to create namespace"
        fi
    else
        log "Namespace $NAMESPACE already exists"
    fi
}

# Update image tag
update_image_tag() {
    if [[ "$IMAGE_TAG" != "latest" ]]; then
        log "Updating image tag to: $IMAGE_TAG"
        # Create temporary kustomization with image tag override
        local temp_kustomization="/tmp/kustomization-$ENVIRONMENT.yaml"
        cp "$KUSTOMIZE_PATH/kustomization.yaml" "$temp_kustomization"
        
        # Update the image tag in the temporary file
        sed -i.bak "s/newTag: .*/newTag: $IMAGE_TAG/" "$temp_kustomization" 2>/dev/null || \
        sed -i "s/newTag: .*/newTag: $IMAGE_TAG/" "$temp_kustomization"
        
        # Use temporary kustomization for deployment
        KUSTOMIZE_PATH="/tmp"
        mv "$temp_kustomization" "/tmp/kustomization.yaml"
        cp -r "k8s/environments/$ENVIRONMENT"/* "/tmp/" 2>/dev/null || true
    fi
}

# Deploy application
deploy_application() {
    log "Deploying Rate Limiter to $ENVIRONMENT environment..."
    
    local apply_args=""
    if [[ "$DRY_RUN" == "true" ]]; then
        apply_args="--dry-run=server"
        log "DRY RUN MODE: No changes will be applied"
    fi
    
    # Apply with kustomize
    kustomize build "$KUSTOMIZE_PATH" | kubectl apply $apply_args -f - || error_exit "Deployment failed"
    
    if [[ "$DRY_RUN" == "true" ]]; then
        log "DRY RUN completed successfully"
        return
    fi
    
    log "Deployment applied successfully"
}

# Wait for deployment to be ready
wait_for_deployment() {
    if [[ "$DRY_RUN" == "true" ]]; then
        return
    fi
    
    log "Waiting for deployment to be ready (timeout: $WAIT_TIMEOUT)..."
    
    # Wait for Redis deployment
    log "Waiting for Redis deployment..."
    kubectl wait --for=condition=available deployment/redis -n "$NAMESPACE" --timeout="$WAIT_TIMEOUT" || error_exit "Redis deployment failed to become ready"
    
    # Wait for Rate Limiter deployment
    log "Waiting for Rate Limiter deployment..."
    kubectl wait --for=condition=available deployment/rate-limiter -n "$NAMESPACE" --timeout="$WAIT_TIMEOUT" || error_exit "Rate Limiter deployment failed to become ready"
    
    log "All deployments are ready"
}

# Verify deployment
verify_deployment() {
    if [[ "$DRY_RUN" == "true" ]]; then
        return
    fi
    
    log "Verifying deployment..."
    
    # Check pod status
    local rate_limiter_pods=$(kubectl get pods -n "$NAMESPACE" -l app.kubernetes.io/name=distributed-rate-limiter --no-headers | wc -l)
    local redis_pods=$(kubectl get pods -n "$NAMESPACE" -l app.kubernetes.io/name=redis --no-headers | wc -l)
    
    log "Rate Limiter pods: $rate_limiter_pods"
    log "Redis pods: $redis_pods"
    
    if [[ $rate_limiter_pods -eq 0 ]]; then
        error_exit "No Rate Limiter pods found"
    fi
    
    if [[ $redis_pods -eq 0 ]]; then
        error_exit "No Redis pods found"
    fi
    
    # Check health endpoint
    log "Checking application health..."
    local rate_limiter_pod=$(kubectl get pods -n "$NAMESPACE" -l app.kubernetes.io/name=distributed-rate-limiter -o jsonpath='{.items[0].metadata.name}')
    
    local max_attempts=30
    local attempt=1
    while [[ $attempt -le $max_attempts ]]; do
        if kubectl exec -n "$NAMESPACE" "$rate_limiter_pod" -- curl -f http://localhost:8080/actuator/health &> /dev/null; then
            log "Health check passed"
            break
        fi
        log "Waiting for health check to pass (attempt $attempt/$max_attempts)..."
        sleep 5
        ((attempt++))
    done
    
    if [[ $attempt -gt $max_attempts ]]; then
        error_exit "Health check failed after $max_attempts attempts"
    fi
    
    log "Deployment verification completed successfully"
}

# Show deployment status
show_status() {
    if [[ "$DRY_RUN" == "true" ]]; then
        return
    fi
    
    log "Deployment Status:"
    echo "===================="
    kubectl get all -n "$NAMESPACE" -l app.kubernetes.io/part-of=distributed-rate-limiter
    echo
    log "Ingress Status:"
    kubectl get ingress -n "$NAMESPACE"
    echo
    log "Recent Events:"
    kubectl get events -n "$NAMESPACE" --sort-by=.metadata.creationTimestamp | tail -10
}

# Cleanup temporary files
cleanup() {
    rm -f /tmp/rate-limiter-manifest-*.yaml
    rm -f /tmp/kustomization.yaml
    rm -f /tmp/*.yaml 2>/dev/null || true
}

# Main execution
main() {
    # Set up cleanup trap
    trap cleanup EXIT
    
    if [[ $# -eq 0 ]] || [[ "${1:-}" == "-h" ]] || [[ "${1:-}" == "--help" ]]; then
        usage
    fi
    
    log "Starting deployment process for environment: $1"
    
    validate_environment
    check_prerequisites
    validate_manifests
    create_namespace
    update_image_tag
    deploy_application
    wait_for_deployment
    verify_deployment
    show_status
    
    log "Deployment completed successfully!"
    log "Environment: $ENVIRONMENT"
    log "Namespace: $NAMESPACE"
    log "Image Tag: $IMAGE_TAG"
}

# Run main function
main "$@"