const apiUrl = import.meta.env.VITE_API_URL?.trim() || 'http://localhost:8080/api/v1'

export const env = {
  apiUrl: apiUrl.replace(/\/$/, ''),
} as const
