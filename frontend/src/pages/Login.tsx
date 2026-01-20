import React, { useState } from 'react';
import { useForm } from 'react-hook-form';
import { useNavigate, Link } from 'react-router-dom';
import api from '../services/api';
import { useAuth } from '../context/AuthContext';
import { LoginRequest } from '../types';
import { getErrorMessage } from '../utils/errorHandler';

const Login: React.FC = () => {
    // -------------------------------------------------------------------------
    // BEGINNER NOTE: react-hook-form
    // A library that makes managing forms easier.
    // - register: Connects an input field to the form library.
    // - handleSubmit: Wraps our submit function to handle validation first.
    // - formState: Contains information like validation errors.
    // -------------------------------------------------------------------------
    const { register, handleSubmit, formState: { errors } } = useForm<LoginRequest>();
    const [loginError, setLoginError] = useState<string>('');
    const navigate = useNavigate(); // Hook to navigate to different pages
    const { login } = useAuth();    // Get 'login' function from our Global Context
    const [isLoading, setIsLoading] = useState(false); // Anti-spam / UX state

    // This function runs only if validation passes
    const onSubmit = async (data: LoginRequest) => {
        setIsLoading(true);
        setLoginError('');
        try {
            // Send data to backend (JWT is set as HttpOnly cookie by server)
            await api.post('/auth/login', data);

            // If successful, update auth state
            login();

            // Redirect to article list
            navigate('/articles');
        } catch (err) {
            setLoginError(getErrorMessage(err));
        } finally {
            setIsLoading(false);
        }
    };

    return (
        <main style={{ maxWidth: '400px', margin: 'auto', padding: '20px' }}>
            <h1>Login</h1>
            {/* handleSubmit(onSubmit) automatically prevents page reload */}
            <form onSubmit={handleSubmit(onSubmit)} aria-label="Login form">
                <div style={{ marginBottom: '10px' }}>
                    <label htmlFor="email">Email:</label>
                    <input
                        id="email"
                        // The 'register' function hooks this input up to React Hook Form
                        // We also define validation rules here (required, pattern)
                        {...register('email', {
                            required: 'Email is required',
                            maxLength: { value: 100, message: "Email is too long" }, // Defense: Prevent huge strings
                            pattern: {
                                value: /^[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}$/i,
                                message: "Invalid email address"
                            }
                        })}
                        type="email"
                        maxLength={100} // Browser-level defense
                        style={{ width: '100%', padding: '8px' }}
                        disabled={isLoading}
                        aria-invalid={errors.email ? 'true' : 'false'}
                        aria-describedby={errors.email ? 'email-error' : undefined}
                        autoComplete="email"
                    />
                    {errors.email && <span id="email-error" style={{ color: 'red' }} role="alert">{errors.email.message as string}</span>}
                </div>
                <div style={{ marginBottom: '10px' }}>
                    <label htmlFor="password">Password:</label>
                    <input
                        id="password"
                        type="password"
                        {...register('password', {
                            required: 'Password is required',
                            maxLength: { value: 72, message: "Password is too long" } // Defense
                        })}
                        maxLength={72} // Browser-level defense
                        style={{ width: '100%', padding: '8px' }}
                        disabled={isLoading}
                        aria-invalid={errors.password ? 'true' : 'false'}
                        aria-describedby={errors.password ? 'password-error' : undefined}
                        autoComplete="current-password"
                    />
                    {errors.password && <span id="password-error" style={{ color: 'red' }} role="alert">{errors.password.message as string}</span>}
                </div>
                {loginError && <div style={{ color: 'red', marginBottom: '10px' }} role="alert" aria-live="polite">{loginError}</div>}
                <button
                    type="submit"
                    style={{ padding: '10px 20px', opacity: isLoading ? 0.7 : 1 }}
                    disabled={isLoading}
                    aria-busy={isLoading}
                >
                    {isLoading ? 'Logging in...' : 'Login'}
                </button>
                <div style={{ marginTop: '10px' }}>
                    <Link to="/register">Don't have an account? Register</Link>
                </div>
            </form>
        </main>
    );
};

export default Login;
