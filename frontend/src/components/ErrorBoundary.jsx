import React from 'react';

class ErrorBoundary extends React.Component {
    constructor(props) {
        super(props);
        this.state = { hasError: false, error: null, errorInfo: null };
    }

    static getDerivedStateFromError(error) {
        return { hasError: true };
    }

    componentDidCatch(error, errorInfo) {
        this.setState({ error, errorInfo });
        console.error("Uncaught error:", error, errorInfo);
    }

    render() {
        if (this.state.hasError) {
            return (
                <div style={{ padding: '2rem', color: 'white', background: '#121212', minHeight: '100vh', fontFamily: 'monospace' }}>
                    <h1 style={{ color: '#ef4444', fontSize: '1.5rem', marginBottom: '1rem' }}>Something went wrong.</h1>
                    <details style={{ whiteSpace: 'pre-wrap' }}>
                        <summary style={{ cursor: 'pointer', color: '#3b82f6' }}>View Error Details</summary>
                        <p style={{ marginTop: '1rem', color: '#f87171' }}>{this.state.error && this.state.error.toString()}</p>
                        <p style={{ color: '#9ca3af' }}>{this.state.errorInfo && this.state.errorInfo.componentStack}</p>
                    </details>
                    <button
                        onClick={() => window.location.reload()}
                        style={{ marginTop: '1rem', padding: '0.5rem 1rem', background: '#3b82f6', border: 'none', borderRadius: '4px', color: 'white', cursor: 'pointer' }}
                    >
                        Refresh Page
                    </button>
                </div>
            );
        }

        return this.props.children;
    }
}

export default ErrorBoundary;
