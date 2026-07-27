import { useState, useEffect } from 'react';
import { motion } from 'framer-motion';
import { Lock, Unlock, Eye, EyeOff } from 'lucide-react';
import { togglePrivateAccount, updateProfileSettings } from '../api/users';
import { useToast } from './Toast';

export default function PrivacyToggle({ profile, onUpdate }) {
    const [isPrivate, setIsPrivate] = useState(profile?.isPrivateAccount || false);
    const [saving, setSaving] = useState(false);
    const toast = useToast();

    // Sync state when profile prop changes (e.g., after re-fetch)
    useEffect(() => {
        setIsPrivate(profile?.isPrivateAccount || false);
    }, [profile?.isPrivateAccount]);

    const handleToggle = async () => {
        setSaving(true);
        try {
            const res = await togglePrivateAccount();
            setIsPrivate(res.data.isPrivateAccount);
            onUpdate?.();
        } catch (err) {
            console.error(err);
            toast.error('Failed to update privacy setting');
        } finally {
            setSaving(false);
        }
    };

    return (
        <div className="space-y-4">
            {/* Private Account Toggle */}
            <div className="flex items-center justify-between p-4 rounded-xl bg-[var(--bg-card)] border border-[var(--border-color)]">
                <div className="flex items-center gap-3">
                    {isPrivate ? (
                        <Lock size={20} className="text-[var(--accent)]" />
                    ) : (
                        <Unlock size={20} className="text-[var(--text-muted)]" />
                    )}
                    <div>
                        <p className="text-[var(--text-primary)] text-sm font-semibold">Private Account</p>
                        <p className="text-[var(--text-muted)] text-xs">
                            {isPrivate ? 'Only approved followers can see your posts' : 'Anyone can see your posts'}
                        </p>
                    </div>
                </div>
                <motion.button
                    whileTap={{ scale: 0.9 }}
                    onClick={handleToggle}
                    disabled={saving}
                    className={`relative w-12 h-7 rounded-full transition-colors cursor-pointer ${isPrivate ? 'bg-[var(--accent)]' : 'bg-[var(--bg-hover)]'
                        }`}
                >
                    <motion.div
                        layout
                        className="absolute top-1 w-5 h-5 bg-white rounded-full shadow"
                        style={{ left: isPrivate ? '24px' : '4px' }}
                    />
                </motion.button>
            </div>

            {/* Story Visibility Info */}
            <div className="flex items-center justify-between p-4 rounded-xl bg-[var(--bg-card)] border border-[var(--border-color)]">
                <div className="flex items-center gap-3">
                    <Eye size={20} className="text-[var(--text-muted)]" />
                    <div>
                        <p className="text-[var(--text-primary)] text-sm font-semibold">Story Visibility</p>
                        <p className="text-[var(--text-muted)] text-xs">
                            Your stories are visible to followers only
                        </p>
                    </div>
                </div>
                <div className="px-3 py-1 text-xs font-medium text-[var(--accent)] bg-[var(--accent-light)] rounded-full">
                    Followers
                </div>
            </div>
        </div>
    );
}
