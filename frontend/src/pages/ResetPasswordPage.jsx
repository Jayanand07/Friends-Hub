
import { useState, useEffect } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { motion } from 'framer-motion';
import { Lock, ArrowRight, Loader, Sparkles, CheckCircle } from 'lucide-react';
import { resetPassword } from '../api/auth';
import { useToast } from '../components/Toast';

export default function ResetPasswordPage() {
    const [searchParams] = useSearchParams();
    const token = searchParams.get('token');
    const navigate = useNavigate();
    const toast = useToast();

    const [password, setPassword] = useState('');
    const [confirmPassword, setConfirmPassword] = useState('');
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');
    const [success, setSuccess] = useState(false);

    useEffect(() => {
        if (!token) {
            toast.error('Invalid link. Redirecting to login...');
            setTimeout(() => navigate('/login'), 3000);
        }
    }, [token, navigate, toast]);

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError('');

        if (password !== confirmPassword) {
            setError('Passwords do not match');
            return;
        }

        if (password.length < 6) {
            setError('Password must be at least 6 characters');
            return;
        }

        setLoading(true);
        try {
            await resetPassword(token, password);
            setSuccess(true);
            toast.success('Password reset successfully! 🔒');
            setTimeout(() => navigate('/login'), 3000);
        } catch (err) {
            const msg = err.response?.data?.message || 'Failed to reset password. Token may be expired.';
            setError(msg);
            toast.error(msg);
        } finally {
            setLoading(false);
        }
    };

    if (!token) return null;

    return (
        <div className="min-h-screen flex items-center justify-center px-4 relative overflow-hidden">
            {/* Orbs - Matching Login Page */}
            <div className="orb orb-1" />
            <div className="orb orb-2" />
            <div className="orb orb-3" />

            <motion.div
                initial={{ opacity: 0, y: 30 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ duration: 0.6, ease: 'easeOut' }}
                className="glass w-full max-w-md rounded-2xl p-8 relative z-10"
            >
                <div className="text-center mb-8">
                    <div className="w-14 h-14 rounded-2xl bg-gradient-to-br from-[var(--accent)] via-purple-500 to-pink-500 flex items-center justify-center mx-auto mb-4 shadow-lg shadow-purple-500/20">
                        {success ? <CheckCircle size={26} className="text-white" /> : <Sparkles size={26} className="text-white" />}
                    </div>
                    <h1 className="text-2xl font-bold text-[var(--text-primary)]">
                        {success ? 'All Set!' : 'Reset Password'}
                    </h1>
                    <p className="text-[12px] text-[var(--text-muted)] mt-1">
                        {success ? 'Your password has been updated' : 'Create a new password for your account'}
                    </p>
                </div>

                {error && (
                    <motion.div
                        initial={{ opacity: 0, y: -8 }}
                        animate={{ opacity: 1, y: 0 }}
                        className="mb-4 p-3 rounded-xl bg-red-500/10 border border-red-500/25 text-red-400 text-[12px]"
                    >
                        {error}
                    </motion.div>
                )}

                {!success && (
                    <form onSubmit={handleSubmit} className="space-y-3.5">
                        <div>
                            <label className="text-[11px] font-medium text-[var(--text-secondary)] mb-1 block">New Password</label>
                            <div className="relative">
                                <Lock size={14} className="absolute left-3 top-1/2 -translate-y-1/2 text-[var(--text-muted)]" />
                                <input
                                    type="password"
                                    className="input-field pl-9 text-[13px]"
                                    placeholder="••••••••"
                                    value={password}
                                    onChange={(e) => setPassword(e.target.value)}
                                    required
                                />
                            </div>
                        </div>

                        <div>
                            <label className="text-[11px] font-medium text-[var(--text-secondary)] mb-1 block">Confirm Password</label>
                            <div className="relative">
                                <Lock size={14} className="absolute left-3 top-1/2 -translate-y-1/2 text-[var(--text-muted)]" />
                                <input
                                    type="password"
                                    className="input-field pl-9 text-[13px]"
                                    placeholder="••••••••"
                                    value={confirmPassword}
                                    onChange={(e) => setConfirmPassword(e.target.value)}
                                    required
                                />
                            </div>
                        </div>

                        <motion.button
                            whileHover={{ scale: 1.01 }}
                            whileTap={{ scale: 0.99 }}
                            type="submit"
                            disabled={loading}
                            className="btn-primary w-full py-3 mt-1"
                        >
                            {loading ? (
                                <><Loader size={15} className="animate-spin" /> Updating...</>
                            ) : (
                                <>Reset Password <ArrowRight size={15} /></>
                            )}
                        </motion.button>
                    </form>
                )}

                {success && (
                    <motion.button
                        initial={{ opacity: 0 }}
                        animate={{ opacity: 1 }}
                        whileHover={{ scale: 1.01 }}
                        whileTap={{ scale: 0.99 }}
                        onClick={() => navigate('/login')}
                        className="btn-primary w-full py-3 mt-4"
                    >
                        Continue to Login
                    </motion.button>
                )}
            </motion.div>
        </div>
    );
}
