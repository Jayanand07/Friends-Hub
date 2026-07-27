import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { motion as Motion } from 'framer-motion';
import { Mail, Lock, Sparkles, ArrowRight, Loader, Eye, EyeOff } from 'lucide-react';
import { login } from '../api/auth';
import { useAuth } from '../context/AuthContext';
import { useToast } from '../components/Toast';
import { GoogleLoginButton } from '../components/GoogleLoginButton';
import { BrandMark } from '../components/BrandMark';

export default function LoginPage() {
    const { loginUser } = useAuth();
    const navigate = useNavigate();
    const toast = useToast();
    const [isForgotPassword, setIsForgotPassword] = useState(false);
    const [resetEmail, setResetEmail] = useState('');
    const [form, setForm] = useState({ email: '', password: '' });
    const [error, setError] = useState('');
    const [loading, setLoading] = useState(false);
    const [showPassword, setShowPassword] = useState(false);

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError('');
        setLoading(true);
        try {
            const res = await login(form);
            loginUser(res.data.token);
            toast.success('Welcome back! 🎉');
            navigate('/');
        } catch (err) {
            const msg = err.response?.data?.message || 'Login failed. Check your credentials.';
            setError(msg);
            toast.error(msg);
        } finally {
            setLoading(false);
        }
    };

    const handleForgotPassword = async (e) => {
        e.preventDefault();
        setError('');
        setLoading(true);
        try {
            const { forgotPassword } = await import('../api/auth');
            await forgotPassword(resetEmail);
            toast.success('Reset link & OTP sent to your email! 📧');
            setIsForgotPassword(false);
            const targetEmail = resetEmail;
            setResetEmail('');
            sessionStorage.setItem('resetPasswordEmail', targetEmail);
            navigate('/reset-password');
        } catch (err) {
            const msg = err.response?.data?.message || 'Failed to send reset link.';
            setError(msg);
            toast.error(msg);
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="min-h-screen flex items-center justify-center px-4 relative overflow-hidden">
            {/* Orbs */}
            <div className="orb orb-1" />
            <div className="orb orb-2" />
            <div className="orb orb-3" />

            <Motion.div
                initial={{ opacity: 0, y: 30 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ duration: 0.6, ease: 'easeOut' }}
                className="glass w-full max-w-md rounded-2xl p-8 relative z-10"
            >
                <div className="text-center mb-8">
                    <BrandMark />
                    <h1 className="text-2xl font-bold text-[var(--text-primary)]">
                        {isForgotPassword ? 'Reset Password' : 'Welcome back'}
                    </h1>
                    <p className="text-[12px] text-[var(--text-muted)] mt-1">
                        {isForgotPassword ? 'Enter your email to receive a reset link' : 'Sign in to your FriendsHub account'}
                    </p>
                </div>

                {error && (
                    <Motion.div
                        initial={{ opacity: 0, y: -8 }}
                        animate={{ opacity: 1, y: 0 }}
                        className="mb-4 p-3 rounded-xl bg-red-500/10 border border-red-500/25 text-red-400 text-[12px]"
                    >
                        {error}
                    </Motion.div>
                )}

                {isForgotPassword ? (
                    <form onSubmit={handleForgotPassword} className="space-y-3.5">
                        <div>
                            <label className="text-[11px] font-medium text-[var(--text-secondary)] mb-1 block">Email</label>
                            <div className="relative">
                                <Mail size={14} className="absolute left-3 top-1/2 -translate-y-1/2 text-[var(--text-muted)]" />
                                <input
                                    type="email"
                                    autoComplete="email"
                                    className="input-field pl-9 text-[13px]"
                                    placeholder="you@example.com"
                                    value={resetEmail}
                                    onChange={(e) => setResetEmail(e.target.value)}
                                    required
                                />
                            </div>
                        </div>

                        <Motion.button
                            whileHover={{ scale: 1.01 }}
                            whileTap={{ scale: 0.99 }}
                            type="submit"
                            disabled={loading}
                            className="btn-primary w-full py-3 mt-1"
                        >
                            {loading ? (
                                <><Loader size={15} className="animate-spin" /> Sending...</>
                            ) : (
                                <>Send Reset Link <ArrowRight size={15} /></>
                            )}
                        </Motion.button>

                        <button
                            type="button"
                            onClick={() => setIsForgotPassword(false)}
                            className="w-full text-center text-[12px] text-[var(--text-muted)] hover:text-[var(--text-primary)] mt-4 block"
                        >
                            Back to Login
                        </button>
                    </form>
                ) : (
                    <form onSubmit={handleSubmit} className="space-y-3.5">
                        <div>
                            <label className="text-[11px] font-medium text-[var(--text-secondary)] mb-1 block">Email</label>
                            <div className="relative">
                                <Mail size={14} className="absolute left-3 top-1/2 -translate-y-1/2 text-[var(--text-muted)]" />
                                <input
                                    type="email"
                                    autoComplete="email"
                                    className="input-field pl-9 text-[13px]"
                                    placeholder="you@example.com"
                                    value={form.email}
                                    onChange={(e) => setForm({ ...form, email: e.target.value })}
                                    required
                                />
                            </div>
                        </div>

                        <div>
                            <div className="flex justify-between items-center mb-1">
                                <label className="text-[11px] font-medium text-[var(--text-secondary)]">Password</label>
                                <button
                                    type="button"
                                    onClick={() => setIsForgotPassword(true)}
                                    className="text-[11px] text-[var(--accent)] hover:underline"
                                >
                                    Forgot Password?
                                </button>
                            </div>
                            <div className="relative">
                                <Lock size={14} className="absolute left-3 top-1/2 -translate-y-1/2 text-[var(--text-muted)]" />
                                <input
                                    id="login-password"
                                    type={showPassword ? 'text' : 'password'}
                                    autoComplete="current-password"
                                    className="input-field pl-9 pr-9 text-[13px]"
                                    placeholder="••••••••"
                                    value={form.password}
                                    onChange={(e) => setForm({ ...form, password: e.target.value })}
                                    required
                                />
                                <button
                                    type="button"
                                    onClick={() => setShowPassword((prev) => !prev)}
                                    className="absolute right-3 top-1/2 -translate-y-1/2 text-[var(--text-muted)] hover:text-[var(--text-primary)]"
                                    aria-label={showPassword ? 'Hide password' : 'Show password'}
                                    aria-controls="login-password"
                                    aria-pressed={showPassword}
                                >
                                    {showPassword ? <EyeOff size={14} /> : <Eye size={14} />}
                                </button>
                            </div>
                        </div>

                        <Motion.button
                            whileHover={{ scale: 1.01 }}
                            whileTap={{ scale: 0.99 }}
                            type="submit"
                            disabled={loading}
                            className="btn-primary w-full py-3 mt-1"
                        >
                            {loading ? (
                                <><Loader size={15} className="animate-spin" /> Signing in...</>
                            ) : (
                                <>Sign In <ArrowRight size={15} /></>
                            )}
                        </Motion.button>
                    </form>
                )}

                {!isForgotPassword && (
                    <>
                        <div className="flex items-center my-6">
                            <div className="flex-1 border-t border-[var(--border)]"></div>
                            <span className="px-3 text-[12px] text-[var(--text-muted)]">OR</span>
                            <div className="flex-1 border-t border-[var(--border)]"></div>
                        </div>

                        <GoogleLoginButton />

                        <p className="text-center text-[12px] text-[var(--text-muted)] mt-6">
                            Don&apos;t have an account?{' '}
                            <Link to="/register" className="text-[var(--accent)] hover:underline font-medium">
                                Sign up
                            </Link>
                        </p>
                    </>
                )}
            </Motion.div>
        </div>
    );
}
