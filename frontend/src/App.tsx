import { Suspense, lazy } from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import Login from './pages/Login';
import PrivateRoute from './components/PrivateRoute';
import ErrorBoundary from './components/ErrorBoundary';

// -----------------------------------------------------------------------------
// BEGINNER NOTE: Lazy Loading
// Instead of loading ALL pages at once (which makes the app slow to start),
// we import them "lazily". They are downloaded only when the user visits them.
// -----------------------------------------------------------------------------
const ArticleList = lazy(() => import('./pages/ArticleList'));
const ArticleForm = lazy(() => import('./pages/ArticleForm'));
const Register = lazy(() => import('./pages/Register'));

function App() {
  return (
    // BEGINNER NOTE: Providers
    // AuthProvider wraps the app so that ANY component inside can access
    // the user's login state (is logged in?) without passing props manually.
    <AuthProvider>
      <ErrorBoundary>
        {/* Router enables navigation between different URLs without reloading the page */}
        <Router>
          {/* Suspense shows a fallback UI (like "Loading...") while "lazy" pages are downloading */}
          <Suspense fallback={<div>Loading...</div>}>
            <Routes>
              {/* PUBLIC ROUTES: Anyone can access these */}
              <Route path="/login" element={<Login />} />
              <Route path="/register" element={<Register />} />
              
              {/* PROTECTED ROUTES: Only logged-in users can access these */}
              {/* The PrivateRoute component checks if user is logged in. */}
              {/* If yes, it renders the child Route (Outlet). If no, it redirects to Login. */}
              <Route element={<PrivateRoute />}>
                <Route path="/articles" element={<ArticleList />} />
                <Route path="/articles/new" element={<ArticleForm />} />
                <Route path="/articles/edit/:id" element={<ArticleForm />} />
              </Route>

              {/* CATCH-ALL: If URL doesn't match anything above, go to /articles */}
              <Route path="*" element={<Navigate to="/articles" />} />
            </Routes>
          </Suspense>
        </Router>
      </ErrorBoundary>
    </AuthProvider>
  );
}

export default App;