import { AxiosError } from 'axios';
import { ERROR_CODES } from '../constants/constants';

interface ApiErrorResponse {
    message?: string;
    error?: string;
    details?: Record<string, string>;
    traceId?: string;
}

interface ParsedError {
    message: string;
    traceId?: string;
    fieldErrors?: Record<string, string>;
}

/**
 * Maps HTTP status codes and error codes to user-friendly messages
 */
export function parseApiError(error: unknown): ParsedError {
    if (!isAxiosError(error)) {
        return {
            message: 'An unexpected error occurred. Please try again.',
        };
    }

    const status = error.response?.status;
    const data = error.response?.data as ApiErrorResponse | undefined;
    const errorCode = data?.error;

    // Handle specific error codes from backend
    if (errorCode === ERROR_CODES.TOKEN_EXPIRED) {
        return {
            message: 'Your session has expired. Please log in again.',
            traceId: data?.traceId,
        };
    }

    if (errorCode === ERROR_CODES.INVALID_TOKEN) {
        return {
            message: 'Your session is invalid. Please log in again.',
            traceId: data?.traceId,
        };
    }

    if (errorCode === ERROR_CODES.RATE_LIMIT_EXCEEDED) {
        return {
            message: 'Too many requests. Please wait a moment and try again.',
            traceId: data?.traceId,
        };
    }

    // Handle HTTP status codes
    switch (status) {
        case 400:
            if (data?.details) {
                return {
                    message: 'Please check the form for errors.',
                    fieldErrors: data.details,
                    traceId: data?.traceId,
                };
            }
            return {
                message: data?.message || 'Invalid request. Please check your input.',
                traceId: data?.traceId,
            };

        case 401:
            return {
                message: 'Authentication failed. Please check your credentials.',
                traceId: data?.traceId,
            };

        case 403:
            return {
                message: "You don't have permission to perform this action.",
                traceId: data?.traceId,
            };

        case 404:
            return {
                message: 'The requested resource was not found.',
                traceId: data?.traceId,
            };

        case 409:
            return {
                message: data?.message || 'A conflict occurred. The data may have been modified.',
                traceId: data?.traceId,
            };

        case 429:
            return {
                message: 'Too many requests. Please wait a moment and try again.',
                traceId: data?.traceId,
            };

        case 500:
        case 502:
        case 503:
        case 504:
            return {
                message: `Something went wrong on our end. ${data?.traceId ? `Reference: ${data.traceId}` : ''}`,
                traceId: data?.traceId,
            };

        default:
            return {
                message: data?.message || 'An unexpected error occurred. Please try again.',
                traceId: data?.traceId,
            };
    }
}

/**
 * Type guard for Axios errors
 */
function isAxiosError(error: unknown): error is AxiosError {
    return (
        typeof error === 'object' &&
        error !== null &&
        'isAxiosError' in error &&
        (error as AxiosError).isAxiosError === true
    );
}

/**
 * Gets a user-friendly error message from any error
 */
export function getErrorMessage(error: unknown): string {
    return parseApiError(error).message;
}
