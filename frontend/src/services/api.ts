import axios from 'axios';

// -----------------------------------------------------------------------------
// BEGINNER NOTE: Axios Instance
// Instead of using 'fetch' everywhere, we create a central "client" for API requests.
// This allows us to set default settings like the Base URL.
// -----------------------------------------------------------------------------
const API_URL = process.env.REACT_APP_API_URL || 'http://localhost:8080/api';

// Helper to read CSRF token from cookie
function getCsrfToken(): string | null {
    const match = document.cookie.match(/XSRF-TOKEN=([^;]+)/);
    return match ? decodeURIComponent(match[1]) : null;
}

const api = axios.create({
    baseURL: API_URL,
    headers: {
        'Content-Type': 'application/json',
    },
    // SECURITY: Use HttpOnly cookies for JWT instead of localStorage
    // This prevents XSS attacks from stealing tokens
    withCredentials: true,
    // SECURITY: Enable XSRF token header (Axios default header name)
    xsrfCookieName: 'XSRF-TOKEN',
    xsrfHeaderName: 'X-XSRF-TOKEN',
});

// -----------------------------------------------------------------------------
// BEGINNER NOTE: Request Interceptor
// Adds CSRF token to all state-changing requests (POST, PUT, DELETE)
// -----------------------------------------------------------------------------
api.interceptors.request.use(
    (config) => {
        const csrfToken = getCsrfToken();
        if (csrfToken && config.method && ['post', 'put', 'delete', 'patch'].includes(config.method.toLowerCase())) {
            config.headers['X-XSRF-TOKEN'] = csrfToken;
        }
        return config;
    },
    (error) => Promise.reject(error)
);

// -----------------------------------------------------------------------------
// BEGINNER NOTE: Response Interceptor
// Handles authentication errors globally - if a request fails due to expired
// or invalid token, we redirect to login.
// -----------------------------------------------------------------------------
api.interceptors.response.use(
    (response) => response,
    (error) => {
        if (error.response?.status === 401) {
            const errorCode = error.response?.data?.error;
            // Handle token expiration
            if (errorCode === 'TOKEN_EXPIRED' || errorCode === 'INVALID_TOKEN') {
                // Dispatch custom event for AuthContext to handle
                window.dispatchEvent(new CustomEvent('auth:logout'));
            }
        }
        return Promise.reject(error);
    }
);

export default api;
