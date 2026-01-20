import { render, screen, waitFor } from '@testing-library/react';
import App from './App';

// Mock the api module
jest.mock('./services/api', () => ({
  __esModule: true,
  default: {
    get: jest.fn().mockRejectedValue({ response: { status: 401 } }),
    post: jest.fn(),
  },
}));

describe('App Component', () => {
  test('renders login page by default when not authenticated', async () => {
    render(<App />);

    // Wait for the component to finish loading
    await waitFor(() => {
      expect(screen.getByRole('heading', { name: /login/i })).toBeInTheDocument();
    });
  });

  test('renders login form with email and password fields', async () => {
    render(<App />);

    await waitFor(() => {
      expect(screen.getByLabelText(/email/i)).toBeInTheDocument();
      expect(screen.getByLabelText(/password/i)).toBeInTheDocument();
      expect(screen.getByRole('button', { name: /login/i })).toBeInTheDocument();
    });
  });

  test('has link to registration page', async () => {
    render(<App />);

    await waitFor(() => {
      expect(screen.getByText(/don't have an account/i)).toBeInTheDocument();
    });
  });
});
