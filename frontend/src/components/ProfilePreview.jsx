import { useAuth } from '../context/AuthContext';
import { useEffect, useState } from 'react';
import { getProfile } from '../api/users';
import { motion } from 'framer-motion';

export default function ProfilePreview() {
    const { user } = useAuth();
    const [profile, setProfile] = useState(null);

    useEffect(() => {
        getProfile().then((res) => setProfile(res.data)).catch(() => { });
    }, []);

    const initial = user?.email?.charAt(0)?.toUpperCase() || '?';
    const displayName = profile ? `${profile.firstName || ''} ${profile.lastName || ''}`.trim() : '';
    const profilePic = profile?.profilePicUrl;

    return (
        <div className="hidden xl:block w-[320px] h-screen sticky top-0 p-5">
            <motion.div
                initial={{ opacity: 0, x: 20 }}
                animate={{ opacity: 1, x: 0 }}
                transition={{ delay: 0.3 }}
                className="pt-4"
            >
                {/* Profile card — IG style */}
                <div className="flex items-center gap-3 mb-6">
                    <div className="avatar-story">
                        <div className="avatar w-[56px] h-[56px] text-[18px]">
                            {profilePic ? (
                                <img src={profilePic} alt={displayName} className="w-full h-full object-cover rounded-full" />
                            ) : initial}
                        </div>
                    </div>
                    <div className="min-w-0">
                        <p className="text-[14px] font-semibold text-[var(--text-primary)] truncate">{displayName || user?.email?.split('@')[0]}</p>
                        <p className="text-[13px] text-[var(--text-muted)] truncate">{displayName ? user?.email?.split('@')[0] : user?.email}</p>
                    </div>
                </div>

                {/* Suggestions header */}
                <div className="flex items-center justify-between mb-3">
                    <span className="text-[13px] font-semibold text-[var(--text-muted)]">Suggested for you</span>
                    <button className="text-[12px] font-semibold text-[var(--text-primary)] hover:text-[var(--text-muted)] cursor-pointer transition-colors">See All</button>
                </div>

                {/* Suggestion placeholders */}
                {[1, 2, 3].map(i => (
                    <div key={i} className="flex items-center gap-3 py-2">
                        <div className="avatar w-8 h-8 text-[10px]">?</div>
                        <div className="flex-1 min-w-0">
                            <p className="text-[13px] font-semibold text-[var(--text-primary)] truncate">suggested_user_{i}</p>
                            <p className="text-[11px] text-[var(--text-muted)] truncate">Suggested for you</p>
                        </div>
                        <button className="text-[12px] font-semibold text-[var(--accent)] hover:text-[var(--accent-hover)] cursor-pointer transition-colors">Follow</button>
                    </div>
                ))}

                {/* Footer */}
                <div className="mt-6 text-[11px] text-[var(--text-muted)]/50 leading-relaxed">
                    <p>
                        <span style={{fontFamily:'Syne,sans-serif', fontWeight:700, fontSize:'13px'}}>
                            <span style={{color:'#FFFFFF'}}>Friends</span>
                            <span style={{color:'#E91E8C'}}>Hub</span>
                        </span> © 2026
                    </p>
                </div>
            </motion.div>
        </div>
    );
}
