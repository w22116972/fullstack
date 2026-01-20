import { FC } from 'react';
import { Navigate, Outlet } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

// -----------------------------------------------------------------------------
// BEGINNER NOTE: Protected Route
// This component acts as a "Gatekeeper".
// 1. It gets the 'isAuthenticated' status from our global AuthContext.
// 2. If loading, it shows a loading state to prevent premature redirects.
// 3. If authenticated, it renders the <Outlet /> (which represents the requested page).
// 4. If not authenticated, it kicks the user back to the "/login" page using <Navigate />.
// -----------------------------------------------------------------------------
const PrivateRoute: FC = () => {
    const { isAuthenticated, isLoading } = useAuth();

    // Wait for auth check to complete before redirecting
    if (isLoading) {
        return <div style={{ padding: '20px', textAlign: 'center' }}>Loading...</div>;
    }

    return isAuthenticated ? <Outlet /> : <Navigate to="/login" />;
};

export default PrivateRoute;
