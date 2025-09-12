#!/bin/bash
set -euo pipefail

# Redis Backup Script for Distributed Rate Limiter
# This script creates automated backups of Redis data with retention management

# Configuration
NAMESPACE="${NAMESPACE:-rate-limiter}"
REDIS_POD_SELECTOR="app.kubernetes.io/name=redis"
BACKUP_DIR="${BACKUP_DIR:-/backups/redis}"
RETENTION_DAYS="${RETENTION_DAYS:-30}"
S3_BUCKET="${S3_BUCKET:-}"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
BACKUP_FILENAME="redis_backup_${TIMESTAMP}.rdb"

# Logging
log() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $*" >&2
}

# Error handling
error_exit() {
    log "ERROR: $1"
    exit 1
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

# Create backup directory
create_backup_dir() {
    log "Creating backup directory: $BACKUP_DIR"
    mkdir -p "$BACKUP_DIR" || error_exit "Failed to create backup directory"
}

# Perform Redis backup
perform_backup() {
    log "Starting Redis backup..."
    
    # Trigger Redis BGSAVE
    log "Triggering Redis background save..."
    kubectl exec -n "$NAMESPACE" "$REDIS_POD" -c redis -- redis-cli BGSAVE || error_exit "Failed to trigger Redis BGSAVE"
    
    # Wait for BGSAVE to complete
    log "Waiting for backup to complete..."
    while true; do
        LASTSAVE=$(kubectl exec -n "$NAMESPACE" "$REDIS_POD" -c redis -- redis-cli LASTSAVE)
        sleep 2
        CURRENT_LASTSAVE=$(kubectl exec -n "$NAMESPACE" "$REDIS_POD" -c redis -- redis-cli LASTSAVE)
        if [[ "$LASTSAVE" != "$CURRENT_LASTSAVE" ]]; then
            log "Background save completed"
            break
        fi
        log "Waiting for background save to complete..."
    done
    
    # Copy RDB file from pod
    log "Copying RDB file from Redis pod..."
    kubectl cp -n "$NAMESPACE" "$REDIS_POD:/data/dump.rdb" "$BACKUP_DIR/$BACKUP_FILENAME" -c redis || error_exit "Failed to copy RDB file"
    
    # Verify backup file
    if [[ ! -f "$BACKUP_DIR/$BACKUP_FILENAME" ]]; then
        error_exit "Backup file not created: $BACKUP_DIR/$BACKUP_FILENAME"
    fi
    
    BACKUP_SIZE=$(stat -f%z "$BACKUP_DIR/$BACKUP_FILENAME" 2>/dev/null || stat -c%s "$BACKUP_DIR/$BACKUP_FILENAME" 2>/dev/null)
    log "Backup completed successfully. File: $BACKUP_FILENAME, Size: $BACKUP_SIZE bytes"
}

# Upload to S3 (optional)
upload_to_s3() {
    if [[ -n "$S3_BUCKET" ]]; then
        log "Uploading backup to S3 bucket: $S3_BUCKET"
        if command -v aws &> /dev/null; then
            aws s3 cp "$BACKUP_DIR/$BACKUP_FILENAME" "s3://$S3_BUCKET/redis-backups/$BACKUP_FILENAME" || log "WARNING: Failed to upload to S3"
        else
            log "WARNING: AWS CLI not available, skipping S3 upload"
        fi
    fi
}

# Clean old backups
cleanup_old_backups() {
    log "Cleaning up backups older than $RETENTION_DAYS days..."
    
    # Local cleanup
    find "$BACKUP_DIR" -name "redis_backup_*.rdb" -type f -mtime +$RETENTION_DAYS -delete || log "WARNING: Failed to clean local backups"
    
    # S3 cleanup (if configured)
    if [[ -n "$S3_BUCKET" ]] && command -v aws &> /dev/null; then
        aws s3 ls "s3://$S3_BUCKET/redis-backups/" | while read -r line; do
            file_date=$(echo "$line" | awk '{print $1}')
            file_name=$(echo "$line" | awk '{print $4}')
            if [[ -n "$file_name" && "$file_name" =~ redis_backup_.*\.rdb ]]; then
                file_age_days=$(( ( $(date +%s) - $(date -d "$file_date" +%s) ) / 86400 ))
                if [[ $file_age_days -gt $RETENTION_DAYS ]]; then
                    log "Deleting old S3 backup: $file_name (${file_age_days} days old)"
                    aws s3 rm "s3://$S3_BUCKET/redis-backups/$file_name" || log "WARNING: Failed to delete S3 backup $file_name"
                fi
            fi
        done
    fi
    
    log "Cleanup completed"
}

# Main execution
main() {
    log "Starting Redis backup process..."
    
    check_prerequisites
    create_backup_dir
    perform_backup
    upload_to_s3
    cleanup_old_backups
    
    log "Redis backup process completed successfully"
    log "Backup file: $BACKUP_DIR/$BACKUP_FILENAME"
}

# Run main function
main "$@"