import DOMPurify from 'dompurify';

/**
 * Sanitizes user-generated content to prevent XSS attacks.
 * Use this for any content that will be rendered in the DOM.
 */
export function sanitizeHtml(dirty: string | null | undefined): string {
    if (!dirty) return '';
    return DOMPurify.sanitize(dirty, {
        ALLOWED_TAGS: ['b', 'i', 'em', 'strong', 'p', 'br', 'ul', 'ol', 'li', 'a'],
        ALLOWED_ATTR: ['href', 'target', 'rel'],
    });
}

/**
 * Sanitizes text content by escaping HTML entities.
 * Use this for plain text that should never contain HTML.
 */
export function escapeHtml(text: string | null | undefined): string {
    if (!text) return '';
    return text
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#039;');
}

/**
 * Strips all HTML tags from content, returning plain text.
 */
export function stripHtml(html: string | null | undefined): string {
    if (!html) return '';
    return DOMPurify.sanitize(html, { ALLOWED_TAGS: [] });
}
