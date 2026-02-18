import { useEffect, useState } from 'react';
import { useSearchParams, Link } from 'react-router-dom';
import { motion } from 'framer-motion';
import { CheckCircle, XCircle, Loader } from 'lucide-react';
import { verifyEmail } from '../api/auth';

export default function VerifyPage() {
    const [params] = useSearchParams();
    const token = params.get('token');
    const [status, setStatus] = useState('loading');
    const [message, setMessage] = useState('');

    useEffect(() => {
        if (!token) {
            setStatus('error');
            setMessage('No verification token provided.');
            return;
        }
        verifyEmail(token)
            .then((res) => {
                setStatus('success');
                setMessage(typeof res.data === 'string' ? res.data : 'Account verified!');
            })
            .catch((err) => {
                setStatus('error');
                setMessage(err.response?.data?.message || 'Verification failed.');
            });
    }, [token]);

    const icons = {
        loading: <Loader size={48} className="text-[var(--accent)] animate-spin" />,
        success: <CheckCircle size={48} className="text-[var(--success)]" />,
        error: <XCircle size={48} className="text-[var(--danger)]" />,
    };

    return (
        <div className="min-h-screen flex items-center justify-center px-4">
            <motion.div
                initial={{ opacity: 0, scale: 0.9 }}
                animate={{ opacity: 1, scale: 1 }}
                className="glass rounded-2xl p-10 text-center max-w-sm"
            >
                <div className="mb-6">{icons[status]}</div>
                <h1 className="text-xl font-bold mb-2">
                    {status === 'loading' ? 'Verifying...' : status === 'success' ? 'Verified!' : 'Error'}
                </h1>
                <p className="text-sm text-[var(--text-muted)] mb-6">{message}</p>
                {status !== 'loading' && (
                    <Link to="/login" className="btn-primary inline-flex">
                        Go to Login
                    </Link>
                )}
            </motion.div>
        </div>
    );
}
