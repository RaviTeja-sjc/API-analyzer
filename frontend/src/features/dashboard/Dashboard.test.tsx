import '@testing-library/jest-dom';
import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import { Dashboard } from './Dashboard';

describe('Dashboard Component', () => {
  it('renders the main title', () => {
    render(
      <BrowserRouter>
        <Dashboard />
      </BrowserRouter>
    );
    expect(screen.getByText('API Analyzer')).toBeInTheDocument();
  });

  it('renders the project cards', () => {
    render(
      <BrowserRouter>
        <Dashboard />
      </BrowserRouter>
    );
    expect(screen.getByText('Payment Gateway API')).toBeInTheDocument();
    expect(screen.getByText('User Service API')).toBeInTheDocument();
  });

  it('renders the View Report button', () => {
    render(
      <BrowserRouter>
        <Dashboard />
      </BrowserRouter>
    );
    const buttons = screen.getAllByText('View Report');
    expect(buttons.length).toBe(2);
  });
});
