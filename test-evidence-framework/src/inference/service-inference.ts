/**
 * Service inference helper - infers services from story content when explicit section is missing
 */

import { ServiceName } from '../models/story-model';

/**
 * Keyword patterns to detect service involvement
 */
const SERVICE_KEYWORDS: Record<ServiceName, string[]> = {
  'frontend': [
    'ui', 'form', 'component', 'react', 'view', 'display', 'dashboard',
    'button', 'input', 'modal', 'table', 'chart', 'responsive'
  ],
  'gateway': [
    'endpoint', 'api', 'rest', 'controller', 'route', 'http',
    'request', 'response', 'validation', 'authentication'
  ],
  'backend': [
    'service', 'repository', 'entity', 'persistence', 'database',
    'business logic', 'calculation', 'workflow', 'domain model'
  ],
  'risk-engine': [
    'pricing', 'valuation', 'risk', 'pv01', 'dv01', 'sensitivity',
    'ore', 'curve', 'scenario', 'monte carlo', 'simulation'
  ]
};

export class ServiceInferenceHelper {
  /**
   * Infer likely services from story content
   */
  inferServices(
    title: string,
    acceptanceCriteria: string[],
    implementationGuidance?: string[],
    deliverables?: string[]
  ): ServiceName[] {
    const content = [
      title,
      ...acceptanceCriteria,
      ...(implementationGuidance || []),
      ...(deliverables || [])
    ].join(' ').toLowerCase();
    
    const inferredServices: ServiceName[] = [];
    const scores: Record<ServiceName, number> = {
      'frontend': 0,
      'backend': 0,
      'gateway': 0,
      'risk-engine': 0
    };
    
    // Count keyword matches
    for (const [service, keywords] of Object.entries(SERVICE_KEYWORDS)) {
      for (const keyword of keywords) {
        const regex = new RegExp(`\\b${keyword}\\b`, 'gi');
        const matches = content.match(regex);
        if (matches) {
          scores[service as ServiceName] += matches.length;
        }
      }
    }
    
    // Services with score > 0 are likely involved
    for (const [service, score] of Object.entries(scores)) {
      if (score > 0) {
        inferredServices.push(service as ServiceName);
      }
    }
    
    // Apply heuristics
    // If frontend is involved, gateway is usually involved too
    if (inferredServices.includes('frontend') && !inferredServices.includes('gateway')) {
      inferredServices.push('gateway');
    }
    
    // If gateway is involved, backend is usually involved too
    if (inferredServices.includes('gateway') && !inferredServices.includes('backend')) {
      inferredServices.push('backend');
    }
    
    return inferredServices;
  }
  
  /**
   * Get confidence level for inferred services
   */
  getConfidence(inferredCount: number): 'high' | 'medium' | 'low' {
    if (inferredCount === 0) return 'low';
    if (inferredCount >= 3) return 'high';
    return 'medium';
  }
}
