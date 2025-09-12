import type { TestScenario } from '../types/config.types';

export const testScenarios: TestScenario[] = [
  {
    id: 'basic-user',
    name: 'Basic Rate Limiting',
    description: 'Simple user making API calls to test basic rate limiting behavior',
    config: {
      key: 'user:123',
      tokens: 1,
      requestCount: 15,
      intervalMs: 1000,
      concurrent: false,
    },
  },
  {
    id: 'burst-traffic',
    name: 'Burst Traffic Simulation',
    description: 'E-commerce flash sale scenario with sudden spike in requests',
    config: {
      key: 'sale:flash',
      tokens: 1,
      requestCount: 50,
      intervalMs: 100,
      concurrent: true,
    },
  },
  {
    id: 'multi-user-fair',
    name: 'Multi-User Fair Usage',
    description: 'Multiple users sharing rate limits with individual tracking',
    config: {
      key: 'user:multiple',
      tokens: 1,
      requestCount: 20,
      intervalMs: 500,
      concurrent: true,
    },
  },
  {
    id: 'api-abuse',
    name: 'API Abuse Detection',
    description: 'Malicious user attempting to overwhelm system with rapid requests',
    config: {
      key: 'abuser:bot',
      tokens: 1,
      requestCount: 100,
      intervalMs: 50,
      concurrent: true,
    },
  },
  {
    id: 'high-volume',
    name: 'High Volume Testing',
    description: 'Test system behavior under sustained high load',
    config: {
      key: 'load:test',
      tokens: 2,
      requestCount: 30,
      intervalMs: 200,
      concurrent: false,
    },
  },
];

export const getScenarioById = (id: string): TestScenario | undefined => {
  return testScenarios.find(scenario => scenario.id === id);
};

export const createCustomScenario = (
  name: string,
  description: string,
  config: TestScenario['config']
): TestScenario => {
  return {
    id: `custom-${Date.now()}`,
    name,
    description,
    config,
  };
};