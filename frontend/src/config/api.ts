// Centralized API base URL configuration.
// Priority: explicit REACT_APP_API_BASE_URL > legacy REACT_APP_API_BASE > default relative path for Docker
// This ensures the frontend talks only to the gateway, not directly to individual services.
const explicit = process.env.REACT_APP_API_BASE_URL;
const legacy = (process as any).env?.REACT_APP_API_BASE; // backward compatibility

// Use relative path by default so nginx can proxy to gateway service
export const API_BASE_URL = (explicit || legacy || '/api').replace(/\/$/, '');

export function apiUrl(path: string): string {
  if (!path.startsWith('/')) path = '/' + path;
  return API_BASE_URL + path;
}

export default API_BASE_URL;
