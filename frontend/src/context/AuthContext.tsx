import React, { createContext, useState, useContext, useEffect, useCallback } from 'react';
import api from '../services/api';

// Define what our Authentication Context looks like
interface AuthContextType {
    isAuthenticated: boolean;
    isLoading: boolean;
    login: () => void;
    logout: () => void;
}

// Create the context. Initially undefined.
const AuthContext = createContext<AuthContextType | undefined>(undefined);

// -----------------------------------------------------------------------------
// BEGINNER NOTE: Context Provider
// This component manages the "global" state of the user (are they logged in?).
// It wraps the rest of the application (see App.tsx) so everyone can access this data.
//
// SECURITY: JWT is now stored in HttpOnly cookies, not localStorage.
// This prevents XSS attacks from stealing the token.
// -----------------------------------------------------------------------------
export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
    // BEGINNER NOTE: State
    // 'isAuthenticated' is the variable. 'setIsAuthenticated' is the function to change it.
    // React re-renders components whenever state changes.
    const [isAuthenticated, setIsAuthenticated] = useState<boolean>(false);
    const [isLoading, setIsLoading] = useState<boolean>(true);

    const logout = useCallback(async () => {
        try {
            await api.post('/auth/logout');
        } catch {
            // Ignore logout errors - cookie will expire anyway
        }
        setIsAuthenticated(false);
    }, []);

    // BEGINNER NOTE: useEffect
    // This runs ONCE when the app starts (because of the empty array [] at the end).
    // It checks if we have a valid session by calling /api/auth/me
    useEffect(() => {
        const checkAuth = async () => {
            try {
                await api.get('/auth/me');
                setIsAuthenticated(true);
            } catch {
                setIsAuthenticated(false);
            } finally {
                setIsLoading(false);
            }
        };
        checkAuth();

        // Listen for auth:logout events from API interceptor
        const handleLogout = () => {
            setIsAuthenticated(false);
        };
        window.addEventListener('auth:logout', handleLogout);
        return () => window.removeEventListener('auth:logout', handleLogout);
    }, []);

    const login = () => {
        // Token is now set via HttpOnly cookie by the server
        // We just update the state to reflect authentication
        setIsAuthenticated(true);
    };

    return (
        // We "provide" the state and functions to all 'children' components
        <AuthContext.Provider value={{ isAuthenticated, isLoading, login, logout }}>
            {children}
        </AuthContext.Provider>
    );
};

// Custom Hook to easily use this context in other files
export const useAuth = () => {
    const context = useContext(AuthContext);
    if (!context) {
        throw new Error('useAuth must be used within an AuthProvider');
    }
    return context;
};
