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
                <div style={{ padding: '2rem', color: 'var(--text-primary)', background: 'var(--bg-primary)', minHeight: '100vh', fontFamily: 'monospace' }}>
                    <h1 style={{ color: 'var(--danger)', fontSize: '1.5rem', marginBottom: '1rem' }}>Something went wrong.</h1>
                    <p style={{ color: 'var(--text-muted)', marginBottom: '1rem' }}>An unexpected error occurred. Please try refreshing the page.</p>
                    <button
                        onClick={() => window.location.reload()}
                        style={{ marginTop: '1rem', padding: '0.5rem 1rem', background: 'var(--accent)', border: 'none', borderRadius: '4px', color: 'white', cursor: 'pointer' }}
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
