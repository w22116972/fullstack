import React, { useState } from 'react';
import { useForm } from 'react-hook-form';
import { useNavigate, Link } from 'react-router-dom';
import api from '../services/api';
import { RegisterRequest } from '../types';

const Register: React.FC = () => {
    const { register, handleSubmit, formState: { errors } } = useForm<RegisterRequest>();
    const [regError, setRegError] = useState<string>('');
    const [isLoading, setIsLoading] = useState(false);
    const navigate = useNavigate();

    const onSubmit = async (data: RegisterRequest) => {
        setIsLoading(true);
        setRegError('');
        try {
            await api.post('/auth/register', data);
            navigate('/login');
        } catch (error: any) {
            // Fix: Extract the 'message' string. If response data is an object, react crashes trying to render it.
            const serverMessage = error.response?.data?.message || error.response?.data || 'Registration failed';
            setRegError(typeof serverMessage === 'string' ? serverMessage : JSON.stringify(serverMessage));
        } finally {
            setIsLoading(false);
        }
    };

    return (
        <div style={{ maxWidth: '400px', margin: 'auto', padding: '20px' }}>
            <h2>Register</h2>
            <form onSubmit={handleSubmit(onSubmit)}>
                <div style={{ marginBottom: '10px' }}>
                    <label>Email:</label>
                    <input
                        {...register('email', { 
                            required: 'Email is required',
                            maxLength: { value: 100, message: "Email is too long" },
                            pattern: {
                                value: /^[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}$/i,
                                message: "Invalid email address"
                            }
                        })}
                        type="email"
                        maxLength={100}
                        style={{ width: '100%', padding: '8px' }}
                        disabled={isLoading}
                    />
                    {errors.email && <span style={{ color: 'red' }}>{errors.email.message as string}</span>}
                </div>
                <div style={{ marginBottom: '10px' }}>
                    <label>Password:</label>
                    <input
                        type="password"
                        {...register('password', { 
                            required: 'Password is required',
                            minLength: { value: 6, message: "Password must be at least 6 characters" },
                            maxLength: { value: 64, message: "Password is too long" }
                        })}
                        maxLength={64}
                        style={{ width: '100%', padding: '8px' }}
                        disabled={isLoading}
                    />
                    {errors.password && <span style={{ color: 'red' }}>{errors.password.message as string}</span>}
                </div>
                {regError && <div style={{ color: 'red', marginBottom: '10px' }}>{regError}</div>}
                <button 
                    type="submit" 
                    style={{ padding: '10px 20px', opacity: isLoading ? 0.7 : 1 }}
                    disabled={isLoading}
                >
                    {isLoading ? 'Registering...' : 'Register'}
                </button>
                <div style={{ marginTop: '10px' }}>
                    <Link to="/login">Already have an account? Login</Link>
                </div>
            </form>
        </div>
    );
};

export default Register;
