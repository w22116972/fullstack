// API paths
export const API_PATHS = {
    AUTH: {
        LOGIN: '/auth/login',
        LOGOUT: '/auth/logout',
        REGISTER: '/auth/register',
        ME: '/auth/me',
    },
    ARTICLES: '/articles',
} as const;

// Error codes from backend
export const ERROR_CODES = {
    TOKEN_EXPIRED: 'TOKEN_EXPIRED',
    INVALID_TOKEN: 'INVALID_TOKEN',
    RATE_LIMIT_EXCEEDED: 'RATE_LIMIT_EXCEEDED',
} as const;

// Validation rules
export const VALIDATION = {
    EMAIL: {
        MAX_LENGTH: 100,
        PATTERN: /^[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}$/i,
    },
    PASSWORD: {
        MIN_LENGTH: 8,
        MAX_LENGTH: 72,
    },
    ARTICLE: {
        TITLE_MAX_LENGTH: 200,
        CONTENT_MAX_LENGTH: 20000,
        TAGS_MAX_LENGTH: 100,
    },
} as const;

// Publish status options
export const PUBLISH_STATUS = {
    DRAFT: 'draft',
    PUBLISHED: 'published',
} as const;

// Pagination
export const PAGINATION = {
    DEFAULT_PAGE_SIZE: 10,
    MAX_PAGE_SIZE: 100,
} as const;
