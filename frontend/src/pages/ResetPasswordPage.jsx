import { useState } from 'react';
import { useNavigate, useSearchParams, Link } from 'react-router-dom';
import { motion as Motion } from 'framer-motion';
import { Lock, Mail, KeyRound, ArrowRight, Loader, CheckCircle, Eye, EyeOff, RefreshCw } from 'lucide-react';
import { resetPassword, forgotPassword } from '../api/auth';
import { useToast } from '../components/Toast';
import { BrandMark } from '../components/BrandMark';

export default function ResetPasswordPage() {
    const [searchParams] = useSearchParams();
    const [email, setEmail] = useState(sessionStorage.getItem('resetPasswordEmail') || searchParams.get('email') || '');
    const [otp, setOtp] = useState('');
    const navigate = useNavigate();
    const toast = useToast();

    const [password, setPassword] = useState('');
    const [confirmPassword, setConfirmPassword] = useState('');
    const [showPassword, setShowPassword] = useState(false);
    const [showConfirmPassword, setShowConfirmPassword] = useState(false);
    const [loading, setLoading] = useState(false);
    const [resending, setResending] = useState(false);
    const [error, setError] = useState('');
    const [passwordError, setPasswordError] = useState('');
    const [success, setSuccess] = useState(false);

    const validatePassword = (pwd) => {
        if (pwd.length < 12) return 'Password must be at least 12 characters';
        if (!/[A-Z]/.test(pwd)) return 'Must contain at least one uppercase letter';
        if (!/[a-z]/.test(pwd)) return 'Must contain at least one lowercase letter';
        if (!/[0-9]/.test(pwd)) return 'Must contain at least one number';
        if (!/[@$!%*?&]/.test(pwd)) return 'Must contain at least one special character (@$!%*?&)';
        return '';
    };

    const passwordStrength =
        password.length >= 12 && /[A-Z]/.test(password) && /[a-z]/.test(password) && /[0-9]/.test(password) && /[@$!%*?&]/.test(password) ? 3 :
        password.length >= 12 ? 2 : password.length > 0 ? 1 : 0;

    const handleResendOtp = async () => {
        if (!email || !email.trim()) {
            setError('Please enter your email to request an OTP');
            return;
        }
        setResending(true);
        setError('');
        try {
            await forgotPassword(email.trim());
            toast.success('New OTP sent to your email! 📧');
        } catch (err) {
            const msg = err.response?.data?.message || 'Failed to resend OTP. Please try again.';
            setError(msg);
            toast.error(msg);
        } finally {
            setResending(false);
        }
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError('');

        if (!otp.trim()) {
            setError('Please enter the 6-digit OTP code sent to your email');
            return;
        }

        const pwdError = validatePassword(password);
        if (pwdError) {
            setPasswordError(pwdError);
            return;
        }
        setPasswordError('');

        if (password !== confirmPassword) {
            setError('Passwords do not match');
            return;
        }

        setLoading(true);
        try {
            await resetPassword(email.trim(), otp.trim(), password);
            setSuccess(true);
            toast.success('Password reset successfully! 🔒');
            sessionStorage.removeItem('resetPasswordEmail');
            setTimeout(() => navigate('/login'), 2500);
        } catch (err) {
            const msg = err.response?.data?.message || 'Failed to reset password. OTP may be invalid or expired.';
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
                        {success ? 'All Set!' : 'Reset Password'}
                    </h1>
                    <p className="text-[12px] text-[var(--text-muted)] mt-1">
                        {success ? 'Your password has been updated' : 'Enter your OTP and create a new secure password'}
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

                {success && (
                    <Motion.div
                        initial={{ opacity: 0, scale: 0.95 }}
                        animate={{ opacity: 1, scale: 1 }}
                        className="mb-4 p-4 rounded-xl bg-emerald-500/10 border border-emerald-500/25 text-emerald-400 text-[13px] flex items-center gap-2"
                    >
                        <CheckCircle size={16} /> Password updated! Redirecting to login...
                    </Motion.div>
                )}

                {!success && (
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
                                    value={email}
                                    onChange={(e) => setEmail(e.target.value)}
                                    required
                                />
                            </div>
                        </div>

                        <div>
                            <div className="flex justify-between items-center mb-1">
                                <label className="text-[11px] font-medium text-[var(--text-secondary)]">6-Digit OTP</label>
                                <button
                                    type="button"
                                    onClick={handleResendOtp}
                                    disabled={resending}
                                    className="text-[11px] text-[var(--accent)] hover:underline flex items-center gap-1"
                                >
                                    {resending ? <Loader size={10} className="animate-spin" /> : <RefreshCw size={10} />}
                                    Resend OTP
                                </button>
                            </div>
                            <div className="relative">
                                <KeyRound size={14} className="absolute left-3 top-1/2 -translate-y-1/2 text-[var(--text-muted)]" />
                                <input
                                    type="text"
                                    maxLength={6}
                                    className="input-field pl-9 text-[13px] tracking-widest font-mono"
                                    placeholder="123456"
                                    value={otp}
                                    onChange={(e) => setOtp(e.target.value.replace(/\D/g, ''))}
                                    required
                                />
                            </div>
                        </div>

                        <div>
                            <label className="text-[11px] font-medium text-[var(--text-secondary)] mb-1 block">New Password</label>
                            <div className="relative">
                                <Lock size={14} className="absolute left-3 top-1/2 -translate-y-1/2 text-[var(--text-muted)]" />
                                <input
                                    type={showPassword ? 'text' : 'password'}
                                    autoComplete="new-password"
                                    className={`input-field pl-9 pr-9 text-[13px] ${passwordError ? 'border-red-500/50' : ''}`}
                                    placeholder="Min. 12 chars, 1 uppercase, 1 special"
                                    value={password}
                                    onChange={(e) => {
                                        setPassword(e.target.value);
                                        if (passwordError) setPasswordError(validatePassword(e.target.value));
                                    }}
                                    required
                                />
                                <button
                                    type="button"
                                    onClick={() => setShowPassword((prev) => !prev)}
                                    className="absolute right-3 top-1/2 -translate-y-1/2 text-[var(--text-muted)] hover:text-[var(--text-primary)]"
                                    aria-label={showPassword ? 'Hide password' : 'Show password'}
                                >
                                    {showPassword ? <EyeOff size={14} /> : <Eye size={14} />}
                                </button>
                            </div>
                            {passwordError && (
                                <p className="text-[11px] text-red-400 mt-1">{passwordError}</p>
                            )}
                            {password.length > 0 && (
                                <div className="flex gap-1 mt-1.5">
                                    {[1, 2, 3].map((level) => (
                                        <div
                                            key={level}
                                            className={`h-1 flex-1 rounded-full transition-colors ${
                                                level <= passwordStrength
                                                    ? passwordStrength === 1
                                                        ? 'bg-red-400'
                                                        : passwordStrength === 2
                                                        ? 'bg-yellow-400'
                                                        : 'bg-emerald-400'
                                                    : 'bg-[var(--border-color)]'
                                            }`}
                                        />
                                    ))}
                                </div>
                            )}
                        </div>

                        <div>
                            <label className="text-[11px] font-medium text-[var(--text-secondary)] mb-1 block">Confirm Password</label>
                            <div className="relative">
                                <Lock size={14} className="absolute left-3 top-1/2 -translate-y-1/2 text-[var(--text-muted)]" />
                                <input
                                    type={showConfirmPassword ? 'text' : 'password'}
                                    autoComplete="new-password"
                                    className="input-field pl-9 pr-9 text-[13px]"
                                    placeholder="••••••••"
                                    value={confirmPassword}
                                    onChange={(e) => setConfirmPassword(e.target.value)}
                                    required
                                />
                                <button
                                    type="button"
                                    onClick={() => setShowConfirmPassword((prev) => !prev)}
                                    className="absolute right-3 top-1/2 -translate-y-1/2 text-[var(--text-muted)] hover:text-[var(--text-primary)]"
                                    aria-label={showConfirmPassword ? 'Hide password' : 'Show password'}
                                >
                                    {showConfirmPassword ? <EyeOff size={14} /> : <Eye size={14} />}
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
                                <><Loader size={15} className="animate-spin" /> Updating...</>
                            ) : (
                                <>Reset Password <ArrowRight size={15} /></>
                            )}
                        </Motion.button>

                        <div className="text-center mt-4">
                            <Link to="/login" className="text-[12px] text-[var(--text-muted)] hover:text-[var(--text-primary)]">
                                Back to Login
                            </Link>
                        </div>
                    </form>
                )}

                {success && (
                    <Motion.button
                        initial={{ opacity: 0 }}
                        animate={{ opacity: 1 }}
                        whileHover={{ scale: 1.01 }}
                        whileTap={{ scale: 0.99 }}
                        onClick={() => navigate('/login')}
                        className="btn-primary w-full py-3 mt-4"
                    >
                        Continue to Login
                    </Motion.button>
                )}
            </Motion.div>
        </div>
    );
}
