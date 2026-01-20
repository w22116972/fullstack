import { VALIDATION } from '../constants/constants';
import { RegisterOptions } from 'react-hook-form';

// Email validation rules for react-hook-form
export const EMAIL_RULES: RegisterOptions = {
    required: 'Email is required',
    maxLength: {
        value: VALIDATION.EMAIL.MAX_LENGTH,
        message: `Email must be less than ${VALIDATION.EMAIL.MAX_LENGTH} characters`,
    },
    pattern: {
        value: VALIDATION.EMAIL.PATTERN,
        message: 'Invalid email address',
    },
};

// Password validation rules for react-hook-form
export const PASSWORD_RULES: RegisterOptions = {
    required: 'Password is required',
    minLength: {
        value: VALIDATION.PASSWORD.MIN_LENGTH,
        message: `Password must be at least ${VALIDATION.PASSWORD.MIN_LENGTH} characters`,
    },
    maxLength: {
        value: VALIDATION.PASSWORD.MAX_LENGTH,
        message: `Password must be less than ${VALIDATION.PASSWORD.MAX_LENGTH} characters`,
    },
};

// Password rules for registration (stronger requirements)
export const REGISTRATION_PASSWORD_RULES: RegisterOptions = {
    ...PASSWORD_RULES,
    validate: {
        hasUppercase: (value: string) =>
            /[A-Z]/.test(value) || 'Password must contain at least one uppercase letter',
        hasLowercase: (value: string) =>
            /[a-z]/.test(value) || 'Password must contain at least one lowercase letter',
        hasDigit: (value: string) =>
            /[0-9]/.test(value) || 'Password must contain at least one digit',
        hasSpecial: (value: string) =>
            /[!@#$%^&*()_+\-=[\]{};':"\\|,.<>/?]/.test(value) ||
            'Password must contain at least one special character',
    },
};

// Article title validation rules
export const TITLE_RULES: RegisterOptions = {
    required: 'Title is required',
    minLength: {
        value: 1,
        message: 'Title is required',
    },
    maxLength: {
        value: VALIDATION.ARTICLE.TITLE_MAX_LENGTH,
        message: `Title must be less than ${VALIDATION.ARTICLE.TITLE_MAX_LENGTH} characters`,
    },
};

// Article content validation rules
export const CONTENT_RULES: RegisterOptions = {
    required: 'Content is required',
    minLength: {
        value: 1,
        message: 'Content is required',
    },
    maxLength: {
        value: VALIDATION.ARTICLE.CONTENT_MAX_LENGTH,
        message: `Content must be less than ${VALIDATION.ARTICLE.CONTENT_MAX_LENGTH} characters`,
    },
};

// Article tags validation rules
export const TAGS_RULES: RegisterOptions = {
    maxLength: {
        value: VALIDATION.ARTICLE.TAGS_MAX_LENGTH,
        message: `Tags must be less than ${VALIDATION.ARTICLE.TAGS_MAX_LENGTH} characters`,
    },
    pattern: {
        value: /^[a-zA-Z0-9,\s-]*$/,
        message: 'Tags can only contain letters, numbers, commas, spaces, and hyphens',
    },
};
