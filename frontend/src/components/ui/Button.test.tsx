import '@testing-library/jest-dom/vitest';
import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { Button } from './Button';

describe('Button Component', () => {
  it('renders correctly with default props', () => {
    render(<Button>Click Me</Button>);
    const button = screen.getByRole('button', { name: /click me/i });
    expect(button).toBeInTheDocument();
    expect(button).toHaveClass('btn', 'btn-primary', 'btn-md');
  });

  it('applies variant and size classes correctly', () => {
    render(<Button variant="danger" size="lg">Delete</Button>);
    const button = screen.getByRole('button', { name: /delete/i });
    expect(button).toHaveClass('btn-danger', 'btn-lg');
  });

  it('renders a loader and disables the button when isLoading is true', () => {
    render(<Button isLoading>Submit</Button>);
    const button = screen.getByRole('button');
    expect(button).toBeDisabled();
    
    // Test that the children are not rendered, but the loader is.
    // Assuming the loader is a span with class "loader"
    const loader = button.querySelector('.loader');
    expect(loader).toBeInTheDocument();
    expect(screen.queryByText(/submit/i)).not.toBeInTheDocument();
  });

  it('disables the button when disabled prop is true', () => {
    render(<Button disabled>Disabled Button</Button>);
    const button = screen.getByRole('button', { name: /disabled button/i });
    expect(button).toBeDisabled();
  });
});
