import { motion } from 'framer-motion';
import { useNavigate } from 'react-router-dom';
import { Home, AlertCircle } from 'lucide-react';

export default function NotFoundPage() {
    const navigate = useNavigate();
    return (
        <div className="min-h-screen flex items-center justify-center px-4 relative overflow-hidden">
            <div className="orb orb-1" />
            <div className="orb orb-2" />
            <motion.div
                initial={{ opacity: 0, y: 30 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ duration: 0.5 }}
                className="glass w-full max-w-md rounded-2xl p-10 relative z-10 text-center"
            >
                <div className="w-20 h-20 rounded-full bg-[var(--accent-light)] flex items-center justify-center mx-auto mb-6">
                    <AlertCircle size={36} className="text-[var(--accent)]" />
                </div>
                <h1 className="text-6xl font-bold text-[var(--text-primary)] mb-2">404</h1>
                <p className="text-[var(--text-secondary)] text-[15px] mb-1">Page not found</p>
                <p className="text-[var(--text-muted)] text-[13px] mb-8">
                    The page you're looking for doesn't exist or has been moved.
                </p>
                <motion.button
                    whileHover={{ scale: 1.02 }}
                    whileTap={{ scale: 0.98 }}
                    onClick={() => navigate('/')}
                    className="btn-primary px-8 py-3 flex items-center gap-2 mx-auto"
                >
                    <Home size={16} />
                    Back to Home
                </motion.button>
            </motion.div>
        </div>
    );
}
