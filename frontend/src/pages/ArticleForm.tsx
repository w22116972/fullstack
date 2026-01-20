import React, { useEffect, useState } from 'react';
import { useForm } from 'react-hook-form';
import { useNavigate, useParams } from 'react-router-dom';
import api from '../services/api';

interface ArticleFormData {
    title: string;
    content: string;
    tags: string;
    publishStatus: 'DRAFT' | 'PUBLISHED';
}

const ArticleForm: React.FC = () => {
    const { register, handleSubmit, setValue, formState: { errors } } = useForm<ArticleFormData>({
        defaultValues: {
            publishStatus: 'DRAFT'
        }
    });
    const navigate = useNavigate();
    const { id } = useParams<{ id: string }>();
    const isEditMode = !!id;
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');

    useEffect(() => {
        if (isEditMode) {
            setLoading(true);
            api.get(`/articles/${id}`).then(response => {
                const data = response.data;
                setValue('title', data.title);
                setValue('content', data.content);
                setValue('tags', data.tags);
                setValue('publishStatus', data.publishStatus);
            }).catch(error => {
                console.error("Failed to load article", error);
                setError("Failed to load article details.");
            }).finally(() => {
                setLoading(false);
            });
        }
    }, [id, isEditMode, setValue]);

    const onSubmit = async (data: ArticleFormData) => {
        setLoading(true);
        setError('');
        try {
            if (isEditMode) {
                await api.put(`/articles/${id}`, data);
            } else {
                await api.post('/articles', data);
            }
            navigate('/articles');
        } catch (error) {
            console.error("Failed to save article", error);
            setError("Failed to save article. Please try again.");
        } finally {
            setLoading(false);
        }
    };

    if (loading && isEditMode && !error) return <div>Loading article...</div>;

    return (
        <div style={{ maxWidth: '600px', margin: 'auto', padding: '20px' }}>
            <h2>{isEditMode ? 'Edit Article' : 'New Article'}</h2>
            {error && <div style={{ color: 'red', marginBottom: '15px' }}>{error}</div>}
            
            <form onSubmit={handleSubmit(onSubmit)}>
                <div style={{ marginBottom: '15px' }}>
                    <label style={{ display: 'block', marginBottom: '5px' }}>Title *</label>
                    <input
                        {...register('title', { 
                            required: 'Title is required',
                            maxLength: { value: 200, message: "Title is too long (max 200 chars)" }
                        })}
                        maxLength={200} // Defensive coding: Browser-level limit
                        style={{ width: '100%', padding: '8px' }}
                        disabled={loading}
                    />
                    {errors.title && <span style={{ color: 'red' }}>{errors.title.message}</span>}
                </div>

                <div style={{ marginBottom: '15px' }}>
                    <label style={{ display: 'block', marginBottom: '5px' }}>Content *</label>
                    <textarea
                        {...register('content', { 
                            required: 'Content is required',
                            maxLength: { value: 20000, message: "Content is too long (max 20000 chars)" }
                        })}
                        maxLength={20000} // Defensive coding
                        style={{ width: '100%', padding: '8px', minHeight: '150px' }}
                        disabled={loading}
                    />
                    {errors.content && <span style={{ color: 'red' }}>{errors.content.message}</span>}
                </div>

                <div style={{ marginBottom: '15px' }}>
                    <label style={{ display: 'block', marginBottom: '5px' }}>Tags (comma separated)</label>
                    <input
                        {...register('tags', {
                            maxLength: { value: 100, message: "Tags are too long (max 100 chars)" }
                        })}
                        maxLength={100} // Defensive coding
                        style={{ width: '100%', padding: '8px' }}
                        disabled={loading}
                    />
                </div>

                <div style={{ marginBottom: '15px' }}>
                    <label style={{ display: 'block', marginBottom: '5px' }}>Status</label>
                    <div>
                        <label style={{ marginRight: '15px' }}>
                            <input
                                type="radio"
                                value="DRAFT"
                                {...register('publishStatus')}
                                disabled={loading}
                            /> Draft
                        </label>
                        <label>
                            <input
                                type="radio"
                                value="PUBLISHED"
                                {...register('publishStatus')}
                                disabled={loading}
                            /> Published
                        </label>
                    </div>
                </div>

                <button type="submit" style={{ padding: '10px 20px' }} disabled={loading}>
                    {loading ? 'Saving...' : (isEditMode ? 'Update' : 'Create')}
                </button>
                <button 
                    type="button" 
                    onClick={() => navigate('/articles')} 
                    style={{ marginLeft: '10px', padding: '10px 20px' }}
                    disabled={loading}
                >
                    Cancel
                </button>
            </form>
        </div>
    );
};

export default ArticleForm;