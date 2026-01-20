import React from 'react';
import ReactDOM from 'react-dom/client';
import './index.css';
import App from './App';
import reportWebVitals from './reportWebVitals';

// -----------------------------------------------------------------------------
// BEGINNER NOTE: Entry Point
// This is the starting point of the React application.
// -----------------------------------------------------------------------------

// 1. We find the HTML element with id="root" (located in public/index.html).
//    This is where our entire React app will live.
const root = ReactDOM.createRoot(
  document.getElementById('root') as HTMLElement
);

// 2. We "render" (display) the Main Component (<App />) inside that root element.
//    <React.StrictMode> helps identify potential problems in the app during development.
root.render(
  <React.StrictMode>
    <App />
  </React.StrictMode>
);

// If you want to start measuring performance in your app, pass a function
// to log results (for example: reportWebVitals(console.log))
// or send to an analytics endpoint. Learn more: https://bit.ly/CRA-vitals
reportWebVitals();
