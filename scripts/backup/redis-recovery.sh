#!/bin/bash
set -euo pipefail

# Redis Recovery Script for Distributed Rate Limiter
# This script restores Redis data from backup files

# Configuration
NAMESPACE="${NAMESPACE:-rate-limiter}"
REDIS_POD_SELECTOR="app.kubernetes.io/name=redis"
BACKUP_FILE="${BACKUP_FILE:-}"
BACKUP_DIR="${BACKUP_DIR:-/backups/redis}"
S3_BUCKET="${S3_BUCKET:-}"
FORCE_RECOVERY="${FORCE_RECOVERY:-false}"

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
Usage: $0 [OPTIONS]

Redis Recovery Script for Distributed Rate Limiter

OPTIONS:
    -f, --file BACKUP_FILE      Path to backup file (required)
    -n, --namespace NAMESPACE   Kubernetes namespace (default: rate-limiter)
    -d, --backup-dir DIR        Backup directory (default: /backups/redis)
    -s, --s3-bucket BUCKET      S3 bucket for remote backups
    -F, --force                 Force recovery without confirmation
    -h, --help                  Show this help message

EXAMPLES:
    $0 -f /backups/redis/redis_backup_20231201_120000.rdb
    $0 -f redis_backup_20231201_120000.rdb -s my-backup-bucket
    $0 --file backup.rdb --namespace rate-limiter-staging --force

EOF
    exit 1
}

# Parse command line arguments
parse_args() {
    while [[ $# -gt 0 ]]; do
        case $1 in
            -f|--file)
                BACKUP_FILE="$2"
                shift 2
                ;;
            -n|--namespace)
                NAMESPACE="$2"
                shift 2
                ;;
            -d|--backup-dir)
                BACKUP_DIR="$2"
                shift 2
                ;;
            -s|--s3-bucket)
                S3_BUCKET="$2"
                shift 2
                ;;
            -F|--force)
                FORCE_RECOVERY="true"
                shift
                ;;
            -h|--help)
                usage
                ;;
            *)
                error_exit "Unknown option: $1"
                ;;
        esac
    done
    
    if [[ -z "$BACKUP_FILE" ]]; then
        error_exit "Backup file is required. Use -f option or set BACKUP_FILE environment variable."
    fi
}

# Check prerequisites
check_prerequisites() {
    log "Checking prerequisites..."
    
    # Check kubectl
    if ! command -v kubectl &> /dev/null; then
        error_exit "kubectl is not installed or not in PATH"
    fi
    
    # Check namespace exists
    if ! kubectl get namespace "$NAMESPACE" &> /dev/null; then
        error_exit "Namespace $NAMESPACE does not exist"
    fi
    
    # Check Redis pod exists
    REDIS_POD=$(kubectl get pods -n "$NAMESPACE" -l "$REDIS_POD_SELECTOR" -o jsonpath='{.items[0].metadata.name}' 2>/dev/null || echo "")
    if [[ -z "$REDIS_POD" ]]; then
        error_exit "No Redis pod found with selector $REDIS_POD_SELECTOR in namespace $NAMESPACE"
    fi
    
    log "Prerequisites check passed. Redis pod: $REDIS_POD"
}

# Download backup from S3 if needed
download_from_s3() {
    if [[ -n "$S3_BUCKET" && ! -f "$BACKUP_FILE" ]]; then
        log "Downloading backup from S3..."
        if command -v aws &> /dev/null; then
            local s3_path="s3://$S3_BUCKET/redis-backups/$(basename "$BACKUP_FILE")"
            local local_path="$BACKUP_DIR/$(basename "$BACKUP_FILE")"
            mkdir -p "$BACKUP_DIR"
            aws s3 cp "$s3_path" "$local_path" || error_exit "Failed to download from S3: $s3_path"
            BACKUP_FILE="$local_path"
        else
            error_exit "AWS CLI not available, cannot download from S3"
        fi
    fi
}

# Verify backup file
verify_backup_file() {
    # Handle relative paths
    if [[ "${BACKUP_FILE:0:1}" != "/" ]]; then
        BACKUP_FILE="$BACKUP_DIR/$BACKUP_FILE"
    fi
    
    if [[ ! -f "$BACKUP_FILE" ]]; then
        error_exit "Backup file not found: $BACKUP_FILE"
    fi
    
    # Basic validation of RDB file
    if ! file "$BACKUP_FILE" | grep -q "Redis RDB file"; then
        log "WARNING: File does not appear to be a valid Redis RDB file"
        if [[ "$FORCE_RECOVERY" != "true" ]]; then
            read -p "Continue anyway? (y/N): " -n 1 -r
            echo
            if [[ ! $REPLY =~ ^[Yy]$ ]]; then
                error_exit "Recovery aborted by user"
            fi
        fi
    fi
    
    local file_size=$(stat -f%z "$BACKUP_FILE" 2>/dev/null || stat -c%s "$BACKUP_FILE" 2>/dev/null)
    log "Backup file verified: $BACKUP_FILE (Size: $file_size bytes)"
}

# Confirm recovery operation
confirm_recovery() {
    if [[ "$FORCE_RECOVERY" == "true" ]]; then
        log "Force recovery mode enabled, skipping confirmation"
        return
    fi
    
    log "WARNING: This operation will replace all data in Redis!"
    log "Namespace: $NAMESPACE"
    log "Redis Pod: $REDIS_POD"
    log "Backup File: $BACKUP_FILE"
    echo
    read -p "Are you sure you want to proceed? (type 'yes' to confirm): " -r
    echo
    if [[ "$REPLY" != "yes" ]]; then
        error_exit "Recovery aborted by user"
    fi
}

# Stop Redis service temporarily
stop_redis() {
    log "Scaling down Redis deployment..."
    kubectl scale deployment redis -n "$NAMESPACE" --replicas=0 || error_exit "Failed to scale down Redis"
    
    # Wait for pod to terminate
    log "Waiting for Redis pod to terminate..."
    while kubectl get pod "$REDIS_POD" -n "$NAMESPACE" &> /dev/null; do
        sleep 2
    done
    log "Redis pod terminated"
}

# Start Redis service
start_redis() {
    log "Scaling up Redis deployment..."
    kubectl scale deployment redis -n "$NAMESPACE" --replicas=1 || error_exit "Failed to scale up Redis"
    
    # Wait for new pod to be ready
    log "Waiting for Redis pod to be ready..."
    kubectl wait --for=condition=ready pod -l "$REDIS_POD_SELECTOR" -n "$NAMESPACE" --timeout=300s || error_exit "Redis pod failed to become ready"
    
    # Get new pod name
    REDIS_POD=$(kubectl get pods -n "$NAMESPACE" -l "$REDIS_POD_SELECTOR" -o jsonpath='{.items[0].metadata.name}')
    log "Redis pod ready: $REDIS_POD"
}

# Restore backup
restore_backup() {
    log "Copying backup file to Redis pod..."
    kubectl cp "$BACKUP_FILE" "$NAMESPACE/$REDIS_POD:/data/dump.rdb" -c redis || error_exit "Failed to copy backup file"
    
    log "Setting correct permissions on backup file..."
    kubectl exec -n "$NAMESPACE" "$REDIS_POD" -c redis -- chown redis:redis /data/dump.rdb || error_exit "Failed to set permissions"
    
    log "Backup file restored successfully"
}

# Verify recovery
verify_recovery() {
    log "Verifying Redis recovery..."
    
    # Wait for Redis to be responsive
    local max_attempts=30
    local attempt=1
    while [[ $attempt -le $max_attempts ]]; do
        if kubectl exec -n "$NAMESPACE" "$REDIS_POD" -c redis -- redis-cli ping &> /dev/null; then
            log "Redis is responsive"
            break
        fi
        log "Waiting for Redis to become responsive (attempt $attempt/$max_attempts)..."
        sleep 2
        ((attempt++))
    done
    
    if [[ $attempt -gt $max_attempts ]]; then
        error_exit "Redis did not become responsive after recovery"
    fi
    
    # Get basic stats
    local key_count=$(kubectl exec -n "$NAMESPACE" "$REDIS_POD" -c redis -- redis-cli DBSIZE 2>/dev/null || echo "0")
    local info_memory=$(kubectl exec -n "$NAMESPACE" "$REDIS_POD" -c redis -- redis-cli INFO memory | grep used_memory_human: | cut -d: -f2 | tr -d '\r')
    
    log "Recovery verification completed"
    log "Database size: $key_count keys"
    log "Memory usage: $info_memory"
}

# Main execution
main() {
    log "Starting Redis recovery process..."
    
    parse_args "$@"
    check_prerequisites
    download_from_s3
    verify_backup_file
    confirm_recovery
    
    stop_redis
    start_redis
    restore_backup
    
    # Restart Redis to load the backup
    log "Restarting Redis to load backup..."
    kubectl delete pod "$REDIS_POD" -n "$NAMESPACE" || error_exit "Failed to restart Redis pod"
    start_redis
    
    verify_recovery
    
    log "Redis recovery process completed successfully"
}

# Run main function
main "$@"