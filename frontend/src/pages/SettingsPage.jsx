import { useState, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { User, Shield, Lock, UserX, Bell, ChevronRight, Loader2, Save, ArrowLeft } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { getProfile, updateProfileSettings } from '../api/users';
import PrivacyToggle from '../components/PrivacyToggle';
import BlockedUsersList from '../components/BlockedUsersList';
import FollowRequestsPanel from '../components/FollowRequestsPanel';
import { useToast } from '../components/Toast';

const TABS = [
    { id: 'profile', label: 'Profile Info', icon: User },
    { id: 'privacy', label: 'Privacy', icon: Lock },
    { id: 'requests', label: 'Follow Requests', icon: Bell },
    { id: 'blocked', label: 'Blocked Users', icon: UserX },
    { id: 'account', label: 'Account Type', icon: Shield },
];

export default function SettingsPage() {
    const navigate = useNavigate();
    const [activeTab, setActiveTab] = useState('profile');
    const [profile, setProfile] = useState(null);
    const [loading, setLoading] = useState(true);
    const [saving, setSaving] = useState(false);
    const [showBlockedModal, setShowBlockedModal] = useState(false);
    const toast = useToast();

    const [form, setForm] = useState({
        firstName: '', lastName: '', bio: '', website: '',
        location: '', gender: '', dob: '', phone: ''
    });

    useEffect(() => {
        loadProfile();
    }, []);

    const loadProfile = async () => {
        try {
            const res = await getProfile();
            setProfile(res.data);
            setForm({
                firstName: res.data.firstName || '',
                lastName: res.data.lastName || '',
                bio: res.data.bio || '',
                website: res.data.website || '',
                location: res.data.location || '',
                gender: res.data.gender || '',
                dob: res.data.dob || '',
                phone: res.data.phone || '',
            });
        } catch (err) {
            console.error(err);
        } finally {
            setLoading(false);
        }
    };

    const handleSave = async () => {
        setSaving(true);
        try {
            await updateProfileSettings(form);
            await loadProfile();
            toast.success('Profile saved successfully! ✅');
        } catch (err) {
            console.error(err);
            const msg = err.response?.data?.message || 'Failed to save profile. Please try again.';
            toast.error(msg);
        } finally {
            setSaving(false);
        }
    };

    if (loading) {
        return (
            <div className="flex justify-center items-center min-h-[60vh]">
                <Loader2 className="animate-spin text-[#a8a8a8]" size={32} />
            </div>
        );
    }

    return (
        <div className="max-w-[935px] mx-auto px-4 py-6">
            {/* Header */}
            <div className="flex items-center gap-3 mb-8">
                <button onClick={() => navigate(-1)} className="text-[var(--text-primary)] hover:text-[var(--text-muted)] cursor-pointer">
                    <ArrowLeft size={24} />
                </button>
                <h1 className="text-[var(--text-primary)] text-2xl font-bold">Settings</h1>
            </div>

            <div className="flex gap-8">
                {/* Sidebar Tabs */}
                <div className="hidden md:block w-[240px] flex-shrink-0">
                    <nav className="space-y-1">
                        {TABS.map(tab => (
                            <button
                                key={tab.id}
                                onClick={() => setActiveTab(tab.id)}
                                className={`w-full flex items-center gap-3 px-4 py-3 rounded-lg text-sm font-medium transition-colors cursor-pointer ${activeTab === tab.id
                                        ? 'bg-[var(--bg-elevated)] text-[var(--text-primary)]'
                                        : 'text-[var(--text-muted)] hover:text-[var(--text-primary)] hover:bg-[var(--bg-primary)]'
                                    }`}
                            >
                                <tab.icon size={18} />
                                {tab.label}
                                <ChevronRight size={14} className="ml-auto opacity-40" />
                            </button>
                        ))}
                    </nav>
                </div>

                {/* Mobile Tab Bar */}
                <div className="md:hidden w-full mb-4 overflow-x-auto scrollbar-hide">
                    <div className="flex gap-2 pb-2">
                        {TABS.map(tab => (
                            <button
                                key={tab.id}
                                onClick={() => setActiveTab(tab.id)}
                                className={`flex items-center gap-2 px-4 py-2 rounded-full text-xs font-semibold whitespace-nowrap transition-colors cursor-pointer ${activeTab === tab.id
                                        ? 'bg-white text-black'
                                        : 'bg-[var(--bg-elevated)] text-[var(--text-muted)]'
                                    }`}
                            >
                                <tab.icon size={14} />
                                {tab.label}
                            </button>
                        ))}
                    </div>
                </div>

                {/* Content Area */}
                <div className="flex-1 min-w-0">
                    <AnimatePresence mode="wait">
                        <motion.div
                            key={activeTab}
                            initial={{ opacity: 0, y: 10 }}
                            animate={{ opacity: 1, y: 0 }}
                            exit={{ opacity: 0, y: -10 }}
                            transition={{ duration: 0.15 }}
                        >
                            {activeTab === 'profile' && (
                                <div className="space-y-5">
                                    <h2 className="text-[var(--text-primary)] text-lg font-semibold mb-4">Edit Profile</h2>

                                    <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                                        <InputField label="First Name" value={form.firstName} onChange={v => setForm(f => ({ ...f, firstName: v }))} />
                                        <InputField label="Last Name" value={form.lastName} onChange={v => setForm(f => ({ ...f, lastName: v }))} />
                                    </div>

                                    <div>
                                        <label className="block text-[var(--text-muted)] text-xs font-semibold mb-1.5">Bio</label>
                                        <textarea
                                            value={form.bio}
                                            onChange={e => setForm(f => ({ ...f, bio: e.target.value }))}
                                            rows={3}
                                            maxLength={150}
                                            className="w-full bg-[var(--bg-primary)] border border-[var(--border-color)] rounded-lg px-3 py-2.5 text-white text-sm outline-none focus:border-[var(--accent)] transition-colors resize-none"
                                            placeholder="Write something about yourself..."
                                        />
                                        <p className="text-right text-[10px] text-[var(--text-muted)] mt-1">{form.bio.length}/150</p>
                                    </div>

                                    <InputField label="Website" value={form.website} onChange={v => setForm(f => ({ ...f, website: v }))} placeholder="https://example.com" />

                                    <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                                        <InputField label="Location" value={form.location} onChange={v => setForm(f => ({ ...f, location: v }))} />
                                        <div>
                                            <label className="block text-[var(--text-muted)] text-xs font-semibold mb-1.5">Gender</label>
                                            <select
                                                value={form.gender}
                                                onChange={e => setForm(f => ({ ...f, gender: e.target.value }))}
                                                className="w-full bg-[var(--bg-primary)] border border-[var(--border-color)] rounded-lg px-3 py-2.5 text-white text-sm outline-none focus:border-[var(--accent)] transition-colors cursor-pointer"
                                            >
                                                <option value="">Prefer not to say</option>
                                                <option value="Male">Male</option>
                                                <option value="Female">Female</option>
                                                <option value="Other">Other</option>
                                            </select>
                                        </div>
                                    </div>

                                    <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                                        <InputField label="Date of Birth" value={form.dob} onChange={v => setForm(f => ({ ...f, dob: v }))} type="date" />
                                        <InputField label="Phone" value={form.phone} onChange={v => setForm(f => ({ ...f, phone: v }))} placeholder="+1 (555) 123-4567" />
                                    </div>

                                    <motion.button
                                        whileTap={{ scale: 0.97 }}
                                        onClick={handleSave}
                                        disabled={saving}
                                        className="w-full py-3 rounded-lg bg-[var(--accent)] text-white font-semibold text-sm hover:bg-[var(--accent-hover)] transition-colors flex items-center justify-center gap-2 cursor-pointer disabled:opacity-50"
                                    >
                                        {saving ? <Loader2 size={16} className="animate-spin" /> : <Save size={16} />}
                                        {saving ? 'Saving...' : 'Save Changes'}
                                    </motion.button>
                                </div>
                            )}

                            {activeTab === 'privacy' && (
                                <div>
                                    <h2 className="text-[var(--text-primary)] text-lg font-semibold mb-4">Privacy Settings</h2>
                                    <PrivacyToggle profile={profile} onUpdate={loadProfile} />
                                </div>
                            )}

                            {activeTab === 'requests' && (
                                <FollowRequestsPanel />
                            )}

                            {activeTab === 'blocked' && (
                                <div>
                                    <h2 className="text-[var(--text-primary)] text-lg font-semibold mb-4">Blocked Users</h2>
                                    <BlockedUsersList onClose={() => setActiveTab('profile')} />
                                </div>
                            )}

                            {activeTab === 'account' && (
                                <div className="space-y-4">
                                    <h2 className="text-[var(--text-primary)] text-lg font-semibold mb-4">Account</h2>
                                    <div className="p-4 rounded-xl bg-[var(--bg-primary)] border border-[var(--border-color)]">
                                        <div className="flex items-center gap-3 mb-2">
                                            <Shield size={20} className="text-[var(--accent)]" />
                                            <p className="text-[var(--text-primary)] text-sm font-semibold">Account Type</p>
                                        </div>
                                        <p className="text-[var(--text-muted)] text-xs ml-8">
                                            {profile?.isPrivateAccount ? 'Private Account' : 'Public Account'}
                                        </p>
                                        <p className="text-[var(--text-muted)] text-[11px] mt-2 ml-8">
                                            You can change your account type in the Privacy tab.
                                        </p>
                                    </div>

                                    <div className="p-4 rounded-xl bg-[var(--bg-primary)] border border-[var(--border-color)]">
                                        <p className="text-[var(--text-primary)] text-sm font-semibold mb-1">Email</p>
                                        <p className="text-[var(--text-muted)] text-sm">{profile?.email}</p>
                                    </div>
                                </div>
                            )}
                        </motion.div>
                    </AnimatePresence>
                </div>
            </div>

            {/* Blocked Users Modal */}
            <AnimatePresence>
                {showBlockedModal && (
                    <BlockedUsersList onClose={() => setShowBlockedModal(false)} />
                )}
            </AnimatePresence>
        </div>
    );
}

function InputField({ label, value, onChange, type = 'text', placeholder = '' }) {
    return (
        <div>
            <label className="block text-[var(--text-muted)] text-xs font-semibold mb-1.5">{label}</label>
            <input
                type={type}
                value={value}
                onChange={e => onChange(e.target.value)}
                placeholder={placeholder}
                className="w-full bg-[var(--bg-primary)] border border-[var(--border-color)] rounded-lg px-3 py-2.5 text-[var(--text-primary)] text-sm outline-none focus:border-[var(--accent)] transition-colors"
            />
        </div>
    );
}
