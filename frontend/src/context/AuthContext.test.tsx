import { FC } from 'react';
import { render, screen, waitFor, act } from '@testing-library/react';
import { AuthProvider, useAuth } from './AuthContext';

// Mock the api module
const mockGet = jest.fn();
const mockPost = jest.fn();

jest.mock('../services/api', () => ({
    __esModule: true,
    default: {
        get: (...args: unknown[]) => mockGet(...args),
        post: (...args: unknown[]) => mockPost(...args),
    },
}));

// Test component that uses the auth context
const TestConsumer: FC = () => {
    const { isAuthenticated, isLoading, login, logout } = useAuth();

    return (
        <div>
            <span data-testid="loading">{isLoading.toString()}</span>
            <span data-testid="authenticated">{isAuthenticated.toString()}</span>
            <button onClick={login}>Login</button>
            <button onClick={logout}>Logout</button>
        </div>
    );
};

describe('AuthContext', () => {
    beforeEach(() => {
        jest.clearAllMocks();
    });

    test('should start with loading state', async () => {
        mockGet.mockImplementation(() => new Promise(() => {})); // Never resolves

        render(
            <AuthProvider>
                <TestConsumer />
            </AuthProvider>
        );

        expect(screen.getByTestId('loading').textContent).toBe('true');
    });

    test('should set authenticated to true when /auth/me succeeds', async () => {
        mockGet.mockResolvedValueOnce({ data: { email: 'test@example.com' } });

        render(
            <AuthProvider>
                <TestConsumer />
            </AuthProvider>
        );

        await waitFor(() => {
            expect(screen.getByTestId('loading').textContent).toBe('false');
        });

        expect(screen.getByTestId('authenticated').textContent).toBe('true');
    });

    test('should set authenticated to false when /auth/me fails', async () => {
        mockGet.mockRejectedValueOnce({ response: { status: 401 } });

        render(
            <AuthProvider>
                <TestConsumer />
            </AuthProvider>
        );

        await waitFor(() => {
            expect(screen.getByTestId('loading').textContent).toBe('false');
        });

        expect(screen.getByTestId('authenticated').textContent).toBe('false');
    });

    test('login should set authenticated to true', async () => {
        mockGet.mockRejectedValueOnce({ response: { status: 401 } });

        render(
            <AuthProvider>
                <TestConsumer />
            </AuthProvider>
        );

        await waitFor(() => {
            expect(screen.getByTestId('loading').textContent).toBe('false');
        });

        act(() => {
            screen.getByText('Login').click();
        });

        expect(screen.getByTestId('authenticated').textContent).toBe('true');
    });

    test('logout should set authenticated to false and call API', async () => {
        mockGet.mockResolvedValueOnce({ data: { email: 'test@example.com' } });
        mockPost.mockResolvedValueOnce({});

        render(
            <AuthProvider>
                <TestConsumer />
            </AuthProvider>
        );

        await waitFor(() => {
            expect(screen.getByTestId('authenticated').textContent).toBe('true');
        });

        await act(async () => {
            screen.getByText('Logout').click();
        });

        expect(mockPost).toHaveBeenCalledWith('/auth/logout');
        expect(screen.getByTestId('authenticated').textContent).toBe('false');
    });

    test('useAuth should throw error when used outside provider', () => {
        // Suppress console.error for this test
        const consoleSpy = jest.spyOn(console, 'error').mockImplementation(() => {});

        expect(() => {
            render(<TestConsumer />);
        }).toThrow('useAuth must be used within an AuthProvider');

        consoleSpy.mockRestore();
    });
});
