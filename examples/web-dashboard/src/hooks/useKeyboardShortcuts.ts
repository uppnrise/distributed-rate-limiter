import { useEffect } from 'react';

interface KeyboardShortcut {
  key: string;
  ctrlKey?: boolean;
  shiftKey?: boolean;
  altKey?: boolean;
  metaKey?: boolean;
  action: () => void;
  description: string;
}

export function useKeyboardShortcuts(shortcuts: KeyboardShortcut[]) {
  useEffect(() => {
    const handleKeyDown = (event: KeyboardEvent) => {
      const shortcut = shortcuts.find(
        (s) =>
          s.key.toLowerCase() === event.key.toLowerCase() &&
          !!s.ctrlKey === event.ctrlKey &&
          !!s.shiftKey === event.shiftKey &&
          !!s.altKey === event.altKey &&
          !!s.metaKey === event.metaKey
      );

      if (shortcut) {
        event.preventDefault();
        shortcut.action();
      }
    };

    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [shortcuts]);
}

// Common keyboard shortcuts for the dashboard
export const dashboardShortcuts: KeyboardShortcut[] = [
  {
    key: 'd',
    altKey: true,
    action: () => window.location.href = '/',
    description: 'Go to Dashboard',
  },
  {
    key: 'a',
    altKey: true,
    action: () => window.location.href = '/algorithms',
    description: 'Go to Algorithms',
  },
  {
    key: 'c',
    altKey: true,
    action: () => window.location.href = '/configuration',
    description: 'Go to Configuration',
  },
  {
    key: 'l',
    altKey: true,
    action: () => window.location.href = '/load-testing',
    description: 'Go to Load Testing',
  },
  {
    key: 'k',
    altKey: true,
    action: () => window.location.href = '/api-keys',
    description: 'Go to API Keys',
  },
  {
    key: '/',
    ctrlKey: true,
    action: () => console.log('Search activated'),
    description: 'Search',
  },
];
