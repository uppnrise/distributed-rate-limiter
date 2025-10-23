import { useEffect, useState } from 'react';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { Badge } from '@/components/ui/badge';
import { CheckCircle2, XCircle, Loader2 } from 'lucide-react';
import { rateLimiterApi } from '@/services/rateLimiterApi';

export const ApiHealthCheck = () => {
  const [status, setStatus] = useState<'checking' | 'healthy' | 'error'>('checking');
  const [redisStatus, setRedisStatus] = useState<string>('unknown');

  useEffect(() => {
    const checkHealth = async () => {
      try {
        const health = await rateLimiterApi.healthCheck();
        if (health.status === 'UP') {
          setStatus('healthy');
          setRedisStatus(health.components.redis.status);
        } else {
          setStatus('error');
        }
      } catch (error) {
        setStatus('error');
        console.error('Health check failed:', error);
      }
    };

    checkHealth();
    const interval = setInterval(checkHealth, 30000); // Check every 30 seconds

    return () => clearInterval(interval);
  }, []);

  if (status === 'checking') {
    return (
      <Alert className="border-muted">
        <Loader2 className="h-4 w-4 animate-spin" />
        <AlertDescription>
          Connecting to backend API...
        </AlertDescription>
      </Alert>
    );
  }

  if (status === 'error') {
    return (
      <Alert variant="destructive">
        <XCircle className="h-4 w-4" />
        <AlertDescription>
          Backend API unavailable. Make sure the server is running on{' '}
          <code className="font-mono">http://localhost:8080</code>
        </AlertDescription>
      </Alert>
    );
  }

  return (
    <Alert className="border-green-500/50 bg-green-500/10">
      <CheckCircle2 className="h-4 w-4 text-green-600" />
      <AlertDescription className="flex items-center gap-3">
        <span>Connected to backend API</span>
        <Badge variant="secondary" className="text-xs">
          Redis: {redisStatus}
        </Badge>
      </AlertDescription>
    </Alert>
  );
};
