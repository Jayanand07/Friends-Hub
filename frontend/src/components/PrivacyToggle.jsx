import { useState } from 'react';
import { motion } from 'framer-motion';
import { Lock, Unlock, Eye, EyeOff } from 'lucide-react';
import { togglePrivateAccount, updateProfileSettings } from '../api/users';

export default function PrivacyToggle({ profile, onUpdate }) {
    const [isPrivate, setIsPrivate] = useState(profile?.isPrivateAccount || false);
    const [saving, setSaving] = useState(false);

    const handleToggle = async () => {
        setSaving(true);
        try {
            const res = await togglePrivateAccount();
            setIsPrivate(res.data.isPrivateAccount);
            onUpdate?.();
        } catch (err) {
            console.error(err);
        } finally {
            setSaving(false);
        }
    };

    return (
        <div className="space-y-4">
            {/* Private Account Toggle */}
            <div className="flex items-center justify-between p-4 rounded-xl bg-[#1a1a1a] border border-[#262626]">
                <div className="flex items-center gap-3">
                    {isPrivate ? (
                        <Lock size={20} className="text-[#0095f6]" />
                    ) : (
                        <Unlock size={20} className="text-[#a8a8a8]" />
                    )}
                    <div>
                        <p className="text-white text-sm font-semibold">Private Account</p>
                        <p className="text-[#a8a8a8] text-xs">
                            {isPrivate ? 'Only approved followers can see your posts' : 'Anyone can see your posts'}
                        </p>
                    </div>
                </div>
                <motion.button
                    whileTap={{ scale: 0.9 }}
                    onClick={handleToggle}
                    disabled={saving}
                    className={`relative w-12 h-7 rounded-full transition-colors cursor-pointer ${isPrivate ? 'bg-[#0095f6]' : 'bg-[#363636]'
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
            <div className="flex items-center justify-between p-4 rounded-xl bg-[#1a1a1a] border border-[#262626]">
                <div className="flex items-center gap-3">
                    <Eye size={20} className="text-[#a8a8a8]" />
                    <div>
                        <p className="text-white text-sm font-semibold">Story Visibility</p>
                        <p className="text-[#a8a8a8] text-xs">
                            Your stories are visible to followers only
                        </p>
                    </div>
                </div>
                <div className="px-3 py-1 text-xs font-medium text-[#0095f6] bg-[#0095f6]/10 rounded-full">
                    Followers
                </div>
            </div>
        </div>
    );
}
