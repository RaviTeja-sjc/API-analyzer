import '@testing-library/jest-dom/vitest';
import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { Card } from './Card';

describe('Card Component', () => {
  it('renders the children correctly', () => {
    render(<Card>Card Content</Card>);
    expect(screen.getByText('Card Content')).toBeInTheDocument();
  });

  it('renders the title and subtitle when provided', () => {
    render(
      <Card title="Main Title" subtitle="Sub Title">
        Card Content
      </Card>
    );
    expect(screen.getByText('Main Title')).toBeInTheDocument();
    expect(screen.getByText('Sub Title')).toBeInTheDocument();
  });

  it('renders actions when provided', () => {
    render(
      <Card actions={<button>Action Button</button>}>
        Card Content
      </Card>
    );
    expect(screen.getByRole('button', { name: /action button/i })).toBeInTheDocument();
  });

  it('applies custom class names correctly', () => {
    render(<Card className="custom-card-class">Content</Card>);
    const cardElement = screen.getByText('Content').closest('.card');
    expect(cardElement).toHaveClass('custom-card-class');
  });

  it('does not render header when title and actions are absent', () => {
    render(<Card>Content</Card>);
    const cardHeader = document.querySelector('.card-header');
    expect(cardHeader).not.toBeInTheDocument();
  });
});
