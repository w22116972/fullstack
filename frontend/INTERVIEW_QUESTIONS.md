# Potential Interview Questions

Based on the current implementation of the Blog Admin System frontend (`@frontend/**`), here is a list of potential technical interview questions. These cover React, Hooks, State Management, Security, and Code Quality.

## ⚛️ React & Component Architecture

### 1. Component State vs. Global State
**Question:** In `ArticleList.tsx`, you use `useState` for articles, while in `AuthContext.tsx`, you use `createContext`. How do you decide when to use local state versus Context/Global state?
**Context:** `frontend/src/pages/ArticleList.tsx`, `frontend/src/context/AuthContext.tsx`
**Talking Points:**
*   **Local State:** Data only relevant to a specific component (e.g., input values, loading flags, specific list data).
*   **Global State (Context):** Data needed by many components at different nesting levels (e.g., User Login Status, Theme, Language).
*   **Prop Drilling:** Context avoids passing props down 5 levels just to reach a button.

### 2. The `useEffect` Hook
**Question:** In `ArticleList.tsx`, you have `useEffect(() => { fetchArticles(); }, [page]);`. What does the `[page]` dependency array do? What happens if you remove it?
**Context:** `frontend/src/pages/ArticleList.tsx`
**Talking Points:**
*   **Dependency Array:** Tells React *when* to re-run the effect. It runs only when the value of `page` changes.
*   **Empty Array `[]`:** Runs once on mount (componentDidMount).
*   **No Array:** Runs on *every* render (dangerous, causes infinite loops with state updates).
*   **Stale Closures:** Ensuring all variables used inside the effect are in the dependency array (or using functional updates).

### 3. Route Protection
**Question:** Explain how `PrivateRoute.tsx` works. Is this secure enough to prevent a user from accessing admin pages?
**Context:** `frontend/src/components/PrivateRoute.tsx`
**Talking Points:**
*   **Mechanism:** It checks `isAuthenticated`. If true, it renders `<Outlet />` (the child page); otherwise, it redirects to `/login`.
*   **Security:** This is **UX Security only**. It hides the UI.
*   **Real Security:** A malicious user can still manually send API requests (Postman/cURL). The *Backend* must enforce the actual security (401/403) regardless of the frontend state.

### 4. Lazy Loading
**Question:** In `App.tsx`, you use `React.lazy` and `Suspense`. Why?
**Context:** `frontend/src/App.tsx`
**Talking Points:**
*   **Code Splitting:** Instead of downloading one massive `bundle.js` (5MB) immediately, it splits the code into smaller chunks (e.g., `ArticleList.chunk.js`).
*   **Performance:** The browser downloads the chunk only when the user navigates to that route. Faster initial load time.
*   **Fallback:** `Suspense` shows a loading spinner while the chunk is fetching.

## 🪝 Hooks & Custom Logic

### 5. Custom Hooks
**Question:** You created `useAuth()` in `AuthContext.tsx`. Why wrap `useContext(AuthContext)` in a custom function?
**Context:** `frontend/src/context/AuthContext.tsx`
**Talking Points:**
*   **Safety:** It includes a check `if (!context) throw Error`. This ensures developers don't accidentally use the hook outside the `<AuthProvider>`.
*   **Abstraction:** Consumers don't need to import the Context object itself, just the hook.

### 6. React Hook Form
**Question:** Why did you choose `react-hook-form` over standard React controlled inputs (e.g., `value={state} onChange={setState}`)?
**Context:** `frontend/src/pages/Login.tsx`
**Talking Points:**
*   **Performance:** RHF is "uncontrolled" by default. Typing in an input doesn't trigger a re-render of the entire form component on every keystroke.
*   **Validation:** Built-in, declarative validation logic (`required`, `pattern`) is easier to manage than writing manual `if (email.length < 5)` logic.

## 🔐 Security & Networking

### 7. Axios Interceptors
**Question:** In `api.ts`, you use an `api.interceptors.request.use`. What problem does this solve?
**Context:** `frontend/src/services/api.ts`
**Talking Points:**
*   **DRY (Don't Repeat Yourself):** We don't have to manually add `headers: { Authorization: token }` to every single `api.get` or `api.post` call in the entire app.
*   **Consistency:** Ensures every request from the `api` instance is authenticated automatically if a token exists.

### 8. Storing Tokens
**Question:** You are storing the JWT in `localStorage`. What are the security risks, and what is the alternative?
**Context:** `frontend/src/context/AuthContext.tsx`
**Talking Points:**
*   **Risk:** **XSS (Cross-Site Scripting)**. If an attacker injects a script into your page, they can read `localStorage` and steal the token.
*   **Alternative:** **HttpOnly Cookies**. These cannot be read by JavaScript.
*   **Why LocalStorage here?** Simpler for this specific implementation/demo. HttpOnly requires stricter Backend/CORS setup (credentials: true).

## 🧩 TypeScript & Code Quality

### 9. TypeScript Interfaces
**Question:** In `types/index.ts`, you defined `Article` and `User` interfaces. Why is strict typing important in a frontend project?
**Context:** `frontend/src/types/index.ts`
**Talking Points:**
*   **Autocomplete:** IDE suggests properties (`article.title`), speeding up dev.
*   **Refactoring:** Renaming a field (e.g., `title` -> `headline`) is safe; the compiler finds all broken references immediately.
*   **Data Integrity:** Prevents trying to access `article.nam` (typo) or passing a string where a number is expected.

### 10. Error Boundaries
**Question:** What is the purpose of `ErrorBoundary.tsx`? Why is it a Class Component?
**Context:** `frontend/src/components/ErrorBoundary.tsx`
**Talking Points:**
*   **Purpose:** Catches JavaScript errors in the child component tree (rendering errors) so the entire app doesn't crash to a white screen.
*   **Class Component:** `componentDidCatch` and `getDerivedStateFromError` lifecycle methods are **not** available as Hooks yet (as of React 19). You *must* use a class component for an Error Boundary.

## ⚡ Performance

### 11. React.StrictMode
**Question:** In `index.tsx`, the app is wrapped in `<React.StrictMode>`. What does this do?
**Context:** `frontend/src/index.tsx`
**Talking Points:**
*   **Double Invocation:** In development, it renders components **twice** to detect side effects (like impure reducers or useEffects missing cleanup).
*   **Warnings:** Warns about deprecated lifecycles or legacy API usage.
*   **Prod:** It has no effect in production builds.

### 12. List Keys
**Question:** In `ArticleList.tsx`, you use `<tr key={article.id}>`. Why is the `key` prop necessary? What happens if you use `index`?
**Context:** `frontend/src/pages/ArticleList.tsx`
**Talking Points:**
*   **Reconciliation:** Helps React identify which items have changed, added, or removed.
*   **Performance:** Minimizes DOM operations.
*   **Index Issue:** If the list order changes (sorting, deleting), using `index` as a key can cause bugs with component state (input fields showing wrong data) or inefficient re-renders.
