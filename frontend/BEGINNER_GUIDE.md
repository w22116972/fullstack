# Beginner's Guide to this React Application

Welcome! This guide is designed to help you understand how this React application works, even if you have zero prior experience with React.

## Key Concepts Used in This Project

### 1. Components
React applications are built using **Components**. Think of a component as a LEGO block. It's a reusable piece of code that represents a part of the User Interface (UI), like a button, a form, or an entire page.
*   **Code:** Look for functions returning HTML-like tags (JSX). Example: `const Button = () => <button>Click me</button>;`

### 2. Props (Properties)
Components can receive data from their parents, similar to how functions receive arguments. These are called **Props**.
*   **Usage:** `<Welcome name="Alice" />` passes "Alice" as a prop named `name` to the `Welcome` component.

### 3. State (`useState`)
State is the memory of a component. If you want a component to "remember" something (like what the user typed, or if a menu is open), you use **State**.
*   **Hook:** `const [count, setCount] = useState(0);`
    *   `count`: The current value.
    *   `setCount`: A function to update the value.

### 4. Effects (`useEffect`)
Effects are used to run code *after* the component renders. This is where we usually fetch data from a server or set up timers.
*   **Hook:** `useEffect(() => { ... }, [dependency]);`
    *   The code inside runs when the component loads or when `dependency` changes.

### 5. Context (`useContext`)
Sometimes we need to share data (like "is the user logged in?") with *many* components without passing it down manually through every layer. **Context** allows us to "broadcast" this data to the whole app.
*   **Example:** `AuthContext` in this project handles user login state globally.

### 6. Hooks
Functions starting with `use` (like `useState`, `useEffect`, `useAuth`) are called **Hooks**. They let you "hook into" React features like state and lifecycle methods.

## Project Structure Explained

*   **`public/`**: Contains static assets like `index.html`. This is the HTML file that loads your React app.
*   **`src/`**: The heart of your application.
    *   **`index.tsx`**: The Entry Point. This is the first file that runs. It grabs the "root" element from `index.html` and puts your React app inside it.
    *   **`App.tsx`**: The Main Component. It defines the routing (which page to show based on the URL).
    *   **`components/`**: Reusable UI pieces (e.g., `PrivateRoute` which checks if you are allowed to see a page).
    *   **`context/`**: Global state managers (e.g., `AuthContext` for login).
    *   **`pages/`**: Components that represent full screens (e.g., `Login`, `ArticleList`).
    *   **`services/`**: Code for communicating with the Backend API (e.g., `api.ts`).
    *   **`types/`**: TypeScript definitions. These describe the "shape" of our data (e.g., what an "Article" object looks like) to help prevent bugs.

## How to Read the Code

1.  Start at **`src/index.tsx`** to see how the app mounts.
2.  Go to **`src/App.tsx`** to see the list of pages and URLs.
3.  Check **`src/pages/`** to see how individual screens are built.
4.  Look at **`src/services/api.ts`** to see how we talk to the server.

Happy Coding!
