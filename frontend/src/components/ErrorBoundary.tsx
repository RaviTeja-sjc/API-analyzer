import { Component } from 'react';
import type { ErrorInfo, ReactNode } from 'react';

interface Props {
  children?: ReactNode;
  fallback?: ReactNode;
}

interface State {
  hasError: boolean;
  errorMsg: string;
}

export class ErrorBoundary extends Component<Props, State> {
  public state: State = {
    hasError: false,
    errorMsg: ''
  };

  public static getDerivedStateFromError(error: Error): State {
    // Update state so the next render will show the fallback UI.
    return { hasError: true, errorMsg: error.message };
  }

  public componentDidCatch(error: Error, errorInfo: ErrorInfo) {
    console.error('Uncaught error:', error, errorInfo);
  }

  public render() {
    if (this.state.hasError) {
      return this.props.fallback || (
        <div style={{ padding: '2rem', textAlign: 'center', color: '#ff4d4f' }}>
          <h2>Something went wrong.</h2>
          <p>The application encountered an unexpected error. Please refresh the page.</p>
          <pre style={{ fontSize: '0.8rem', background: '#111', padding: '1rem', borderRadius: '4px' }}>
            {this.state.errorMsg}
          </pre>
          <button 
            onClick={() => window.location.reload()}
            style={{ padding: '0.5rem 1rem', marginTop: '1rem', background: '#3b82f6', color: '#fff', border: 'none', borderRadius: '4px', cursor: 'pointer' }}
          >
            Reload Application
          </button>
        </div>
      );
    }

    return this.props.children;
  }
}
