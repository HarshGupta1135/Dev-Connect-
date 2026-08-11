import axios from 'axios';
import { isExpired } from '../utils/jwt';

export const TOKEN_KEY = 'devconnect.token';
export const USER_KEY = 'devconnect.user';

/**
 * Base URL is empty during development on purpose: requests go to the Vite dev
 * server, which proxies /api, /admin and /health to Spring Boot on 8080. That
 * keeps everything same-origin, so no CORS setup is needed in the backend.
 */
const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '',
  headers: { 'Content-Type': 'application/json' },
  // Spring's @RequestParam List<String> expects skills=a&skills=b.
  // Axios would otherwise send skills[]=a&skills[]=b, which does not bind.
  paramsSerializer: { indexes: null },
});

/** Attaches the stored JWT to every outgoing request. */
api.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY);
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

let onUnauthorized = null;

/** Lets AuthContext react to a rejected token without importing React here. */
export function setUnauthorizedHandler(handler) {
  onUnauthorized = handler;
}

api.interceptors.response.use(
  (response) => response,
  (error) => {
    const status = error?.response?.status;
    const url = error?.config?.url || '';
    // Login failures answer 401 too; those are handled by the login form itself.
    const isAuthCall = url.includes('/api/auth/');

    if (!isAuthCall && onUnauthorized) {
      const token = localStorage.getItem(TOKEN_KEY);

      if (status === 401 || status === 403) {
        onUnauthorized(status);
      } else if (status === 500 && token && isExpired(token)) {
        /*
         * The API rejects a lapsed token inside its JWT filter, which sits outside
         * the exception handler, so the failure arrives as a 500 rather than a 401.
         * Checking the token's own expiry tells us what that 500 really means, and
         * the user gets "please sign in again" instead of "unexpected error".
         */
        onUnauthorized(status);
      }
    }

    return Promise.reject(error);
  }
);

/**
 * Every controller wraps its payload in ApiResponse { success, message, data }.
 */
export function unwrap(response) {
  const body = response?.data;
  if (body && typeof body === 'object' && 'data' in body) return body.data;
  return body;
}

export function messageOf(response, fallback = 'Done') {
  return response?.data?.message || fallback;
}

/**
 * Turns a backend failure into something worth showing a user.
 *
 * The API reports several business rule violations through its catch-all
 * handler, which prefixes them with "An unexpected error occurred:". That
 * prefix is stripped so the actual reason is what the user reads.
 */
export function errorMessage(error, fallback = 'Something went wrong. Please try again.') {
  if (error?.code === 'ERR_NETWORK') {
    return 'Cannot reach the server. Is the Spring Boot app running on port 8080?';
  }

  const body = error?.response?.data;
  let message =
    (typeof body === 'string' && body) ||
    body?.message ||
    body?.error ||
    error?.message ||
    fallback;

  message = String(message).replace(/^An unexpected error occurred:\s*/i, '').trim();

  // Spring's default error body for unhandled cases carries no useful text.
  if (!message || message === 'Internal Server Error') return fallback;
  return message;
}

export default api;
