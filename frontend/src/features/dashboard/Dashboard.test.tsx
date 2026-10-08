import '@testing-library/jest-dom/vitest';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import { Dashboard } from './Dashboard';
import { apiClient } from '../../api/client';

vi.mock('../../api/client', () => ({
  apiClient: {
    get: vi.fn(),
  }
}));

describe('Dashboard Component', () => {
  beforeEach(() => {
    (apiClient.get as any).mockResolvedValue({
      data: [
        { id: '1', name: 'Payment Gateway API', status: 'COMPLETED' },
        { id: '2', name: 'User Service API', status: 'COMPLETED' }
      ]
    });
  });

  it('renders the main title', () => {
    render(
      <BrowserRouter>
        <Dashboard />
      </BrowserRouter>
    );
    expect(screen.getByText('API Analyzer')).toBeInTheDocument();
  });

  it('renders the project cards', async () => {
    render(
      <BrowserRouter>
        <Dashboard />
      </BrowserRouter>
    );
    await waitFor(() => {
      expect(screen.getByText('Payment Gateway API')).toBeInTheDocument();
      expect(screen.getByText('User Service API')).toBeInTheDocument();
    });
  });

  it('renders the View Report button', async () => {
    render(
      <BrowserRouter>
        <Dashboard />
      </BrowserRouter>
    );
    await waitFor(() => {
      const buttons = screen.getAllByText('View Report');
      expect(buttons.length).toBe(2);
    });
  });
});
