import { useState } from 'react';
import { Link } from 'react-router-dom';
import { motion } from 'framer-motion';
import { Mail, Lock, UserPlus, ArrowRight, User as UserIcon, Loader, CheckCircle } from 'lucide-react';
import { register } from '../api/auth';
import { useToast } from '../components/Toast';

export default function RegisterPage() {
    const [form, setForm] = useState({ email: '', password: '', firstName: '', lastName: '' });
    const [error, setError] = useState('');
    const [success, setSuccess] = useState('');
    const [loading, setLoading] = useState(false);
    const toast = useToast();

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError('');
        setSuccess('');
        setLoading(true);
        try {
            const res = await register(form);
            const msg = typeof res.data === 'string' ? res.data : 'Registration successful! Check your email.';
            setSuccess(msg);
            toast.success(msg);
        } catch (err) {
            const msg = err.response?.data?.message || 'Registration failed.';
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

            <motion.div
                initial={{ opacity: 0, y: 30 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ duration: 0.6 }}
                className="glass w-full max-w-md rounded-2xl p-8 relative z-10"
            >
                <div className="text-center mb-8">
                    <div style={{ display:'flex', flexDirection:'column', alignItems:'center', gap:'12px', marginBottom:'32px' }}>
                      <div style={{
                        width:'56px', height:'56px', borderRadius:'16px',
                        background:'#E91E8C', position:'relative', overflow:'hidden',
                        boxShadow:'0 8px 24px rgba(233,30,140,0.4)'
                      }}>
                        <div style={{
                          position:'absolute', width:'34px', height:'30px',
                          background:'#000000', borderRadius:'8px 8px 8px 3px',
                          top:'10px', left:'9px'
                        }}/>
                        <div style={{
                          position:'absolute', width:'34px', height:'30px',
                          background:'rgba(0,0,0,0.3)', borderRadius:'8px 8px 3px 8px',
                          bottom:'10px', right:'9px'
                        }}/>
                      </div>
                      <span style={{
                        fontFamily:'Syne, sans-serif', fontWeight:700,
                        fontSize:'28px', letterSpacing:'-0.02em'
                      }}>
                        <span style={{color:'#FFFFFF'}}>Friends</span>
                        <span style={{color:'#E91E8C'}}>Hub</span>
                      </span>
                    </div>
                    <h1 className="text-2xl font-bold">Create account</h1>
                    <p className="text-[12px] text-[var(--text-muted)] mt-1">Join the FriendsHub community</p>
                </div>

                {error && (
                    <motion.div initial={{ opacity: 0, y: -8 }} animate={{ opacity: 1, y: 0 }}
                        className="mb-4 p-3 rounded-xl bg-red-500/10 border border-red-500/25 text-red-400 text-[12px]">{error}</motion.div>
                )}
                {success && (
                    <motion.div initial={{ opacity: 0, y: -8 }} animate={{ opacity: 1, y: 0 }}
                        className="mb-4 p-3 rounded-xl bg-emerald-500/10 border border-emerald-500/25 text-emerald-400 text-[12px] flex items-center gap-2">
                        <CheckCircle size={14} /> {success}
                    </motion.div>
                )}

                <form onSubmit={handleSubmit} className="space-y-3.5">
                    <div className="grid grid-cols-2 gap-3">
                        <div>
                            <label className="text-[11px] font-medium text-[var(--text-secondary)] mb-1 block">First Name</label>
                            <div className="relative">
                                <UserIcon size={14} className="absolute left-3 top-1/2 -translate-y-1/2 text-[var(--text-muted)]" />
                                <input type="text" className="input-field pl-9 text-[13px]" placeholder="John" value={form.firstName}
                                    onChange={(e) => setForm({ ...form, firstName: e.target.value })} required />
                            </div>
                        </div>
                        <div>
                            <label className="text-[11px] font-medium text-[var(--text-secondary)] mb-1 block">Last Name</label>
                            <input type="text" className="input-field text-[13px]" placeholder="Doe" value={form.lastName}
                                onChange={(e) => setForm({ ...form, lastName: e.target.value })} required />
                        </div>
                    </div>

                    <div>
                        <label className="text-[11px] font-medium text-[var(--text-secondary)] mb-1 block">Email</label>
                        <div className="relative">
                            <Mail size={14} className="absolute left-3 top-1/2 -translate-y-1/2 text-[var(--text-muted)]" />
                            <input type="email" className="input-field pl-9 text-[13px]" placeholder="you@example.com" value={form.email}
                                onChange={(e) => setForm({ ...form, email: e.target.value })} required />
                        </div>
                    </div>

                    <div>
                        <label className="text-[11px] font-medium text-[var(--text-secondary)] mb-1 block">Password</label>
                        <div className="relative">
                            <Lock size={14} className="absolute left-3 top-1/2 -translate-y-1/2 text-[var(--text-muted)]" />
                            <input type="password" className="input-field pl-9 text-[13px]" placeholder="••••••••" value={form.password}
                                onChange={(e) => setForm({ ...form, password: e.target.value })} required />
                        </div>
                    </div>

                    <motion.button whileHover={{ scale: 1.01 }} whileTap={{ scale: 0.99 }} type="submit" disabled={loading}
                        className="btn-primary w-full py-3 mt-1">
                        {loading ? (
                            <><Loader size={15} className="animate-spin" /> Creating account...</>
                        ) : (
                            <>Create Account <ArrowRight size={15} /></>
                        )}
                    </motion.button>
                </form>

                <p className="text-center text-[12px] text-[var(--text-muted)] mt-6">
                    Already have an account?{' '}
                    <Link to="/login" className="text-[var(--accent)] hover:underline font-medium">Sign in</Link>
                </p>
            </motion.div>
        </div>
    );
}
