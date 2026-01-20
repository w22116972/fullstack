import React, { useEffect, useState } from 'react';
import api from '../services/api';
import { Article } from '../types';
import { Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { escapeHtml } from '../utils/sanitize';
import { ArticleTableSkeleton } from '../components/Skeleton';
import { getErrorMessage } from '../utils/errorHandler';

const ArticleList: React.FC = () => {
    // -------------------------------------------------------------------------
    // BEGINNER NOTE: Local State
    // We use useState to hold data that affects what the user sees.
    // When these change, the component "re-renders" (updates the UI).
    // -------------------------------------------------------------------------
    const [articles, setArticles] = useState<Article[]>([]);
    const [search, setSearch] = useState('');
    const [page, setPage] = useState(0);
    const [totalPages, setTotalPages] = useState(0);
    const [loading, setLoading] = useState(false);
    const [deletingIds, setDeletingIds] = useState<Set<number>>(new Set());
    const [error, setError] = useState('');
    const { logout } = useAuth(); // Access the logout function from AuthContext

    // Function to fetch data from the Backend API
    const fetchArticles = async () => {
        setLoading(true); // Show loading state
        setError('');     // Clear previous errors
        try {
            // Send GET request to /articles with query parameters
            const response = await api.get(`/articles?title=${search}&page=${page}&size=5`);
            
            // Update state with the data received from server
            setArticles(response.data.content);
            setTotalPages(response.data.totalPages);
        } catch (err) {
            console.error("Failed to fetch articles", err);
            setError(getErrorMessage(err));
        } finally {
            setLoading(false); // Hide loading state regardless of success/failure
        }
    };

    // -------------------------------------------------------------------------
    // BEGINNER NOTE: useEffect Hook
    // This tells React: "Run this code when the component shows up, OR when
    // specific variables change."
    // Here, we re-fetch articles whenever the 'page' variable changes.
    // -------------------------------------------------------------------------
    useEffect(() => {
        fetchArticles();
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [page]); // Dependency array: Effect runs when 'page' changes

    const handleDelete = async (id: number) => {
        if (window.confirm('Are you sure you want to delete this article?')) {
            setDeletingIds(prev => new Set(prev).add(id));
            try {
                await api.delete(`/articles/${id}`);
                fetchArticles(); // Refresh the list after deleting
            } catch (err) {
                console.error("Failed to delete article", err);
                setError(getErrorMessage(err));
            } finally {
                setDeletingIds(prev => {
                    const next = new Set(prev);
                    next.delete(id);
                    return next;
                });
            }
        }
    };

    // Handle search form submission
    const handleSearch = (e: React.FormEvent) => {
        e.preventDefault(); // Prevent default browser form reload
        setPage(0);         // Reset to first page
        fetchArticles();    // Fetch with new search term
    };

    // -------------------------------------------------------------------------
    // BEGINNER NOTE: Rendering (JSX)
    // This is what the user actually sees. It looks like HTML, but it's JavaScript.
    // We use curly braces {} to insert JavaScript variables or logic.
    // -------------------------------------------------------------------------
    return (
        <div style={{ padding: '20px' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '20px' }}>
                <h2 style={{ margin: 0 }}>Articles</h2>
                <button 
                    onClick={logout} 
                    style={{ backgroundColor: '#dc3545', color: 'white', border: 'none', padding: '8px 16px', cursor: 'pointer' }}
                >
                    Logout
                </button>
            </div>
            
            <div style={{ marginBottom: '20px', display: 'flex', justifyContent: 'space-between' }}>
                <form onSubmit={handleSearch}>
                    <input
                        type="text"
                        placeholder="Search by title..."
                        value={search}
                        // Update 'search' state as user types
                        onChange={(e) => setSearch(e.target.value)}
                        style={{ padding: '5px' }}
                    />
                    <button type="submit" style={{ marginLeft: '10px' }}>Search</button>
                </form>
                <Link to="/articles/new">
                    <button>Add New Article</button>
                </Link>
            </div>

            {/* Conditional Rendering: Show specific UI based on state */}
            {loading ? (
                <ArticleTableSkeleton rows={5} />
            ) : error ? (
                <div style={{ color: 'red' }}>{error}</div>
            ) : articles.length === 0 ? (
                <div>No articles found.</div>
            ) : (
                <>
                    <table
                        style={{ width: '100%', borderCollapse: 'collapse' }}
                        role="table"
                        aria-label="Articles list"
                    >
                        <thead>
                            <tr style={{ borderBottom: '1px solid #ccc' }}>
                                <th scope="col" style={{ textAlign: 'left', padding: '10px' }}>Title</th>
                                <th scope="col" style={{ textAlign: 'left', padding: '10px' }}>Author</th>
                                <th scope="col" style={{ textAlign: 'left', padding: '10px' }}>Status</th>
                                <th scope="col" style={{ textAlign: 'left', padding: '10px' }}>Created At</th>
                                <th scope="col" style={{ textAlign: 'left', padding: '10px' }}>Actions</th>
                            </tr>
                        </thead>
                        <tbody>
                            {/* Loop through 'articles' array and create a row for each */}
                            {articles.map(article => (
                                <tr key={article.id} style={{ borderBottom: '1px solid #eee' }}>
                                    <td style={{ padding: '10px' }}>{escapeHtml(article.title)}</td>
                                    <td style={{ padding: '10px' }}>{escapeHtml(article.authorEmail)}</td>
                                    <td style={{ padding: '10px' }}>{escapeHtml(article.publishStatus)}</td>
                                    <td style={{ padding: '10px' }}>{new Date(article.createdAt).toLocaleDateString()}</td>
                                    <td style={{ padding: '10px' }}>
                                        <Link
                                            to={`/articles/edit/${article.id}`}
                                            style={{ marginRight: '10px' }}
                                            aria-label={`Edit article: ${article.title}`}
                                        >
                                            Edit
                                        </Link>
                                        <button
                                            onClick={() => handleDelete(article.id)}
                                            style={{ color: 'red', cursor: deletingIds.has(article.id) ? 'not-allowed' : 'pointer', opacity: deletingIds.has(article.id) ? 0.5 : 1 }}
                                            disabled={deletingIds.has(article.id)}
                                            aria-label={`Delete article: ${article.title}`}
                                            aria-busy={deletingIds.has(article.id)}
                                        >
                                            {deletingIds.has(article.id) ? 'Deleting...' : 'Delete'}
                                        </button>
                                    </td>
                                </tr>
                            ))}
                        </tbody>
                    </table>

                    <div style={{ marginTop: '20px', display: 'flex', justifyContent: 'center', gap: '10px' }}>
                        <button disabled={page === 0} onClick={() => setPage(page - 1)}>Previous</button>
                        <span>Page {page + 1} of {totalPages}</span>
                        <button disabled={page === totalPages - 1} onClick={() => setPage(page + 1)}>Next</button>
                    </div>
                </>
            )}
        </div>
    );
};

export default ArticleList;