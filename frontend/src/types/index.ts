export interface User {
    email: string;
}

export interface Article {
    id: number;
    title: string;
    content: string;
    tags: string;
    publishStatus: 'draft' | 'published';
    authorEmail: string;
    createdAt: string;
    updatedAt: string;
}

export interface LoginResponse {
    token: string;
}

export interface LoginRequest {
    email: string;
    password: string;
}

export interface RegisterRequest {
    email: string;
    password: string;
}
