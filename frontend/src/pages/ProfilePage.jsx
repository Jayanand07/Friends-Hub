import { useState, useEffect, useRef } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { motion, AnimatePresence } from 'framer-motion';
import { Settings, Users, MapPin, Calendar, Loader, Camera, Grid3X3, Lock, UserPlus, UserCheck, UserX } from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { getProfile, getUserProfileById, updateProfile, getFollowers, getFollowing, uploadProfilePic, removeProfilePicture, followUser, unfollowUser } from '../api/users';
import { getPostsByUser } from '../api/posts';
import { useToast } from '../components/Toast';
import FollowersModal from '../components/FollowersModal';

function StatItem({ count, label, onClick }) {
    return (
        <button onClick={onClick} className="flex flex-col items-center gap-0 cursor-pointer group">
            <span className="text-[16px] font-bold text-[var(--text-primary)]">{count}</span>
            <span className="text-[13px] text-[var(--text-secondary)] group-hover:text-[var(--text-primary)] transition-colors">{label}</span>
        </button>
    );
}

export default function ProfilePage() {
    const { user: currentUser } = useAuth();
    const { userId } = useParams();
    const navigate = useNavigate();
    const [profile, setProfile] = useState(null);
    const [editing, setEditing] = useState(false);
    const [form, setForm] = useState({ firstName: '', lastName: '', bio: '', city: '' });
    const [loading, setLoading] = useState(true);
    const [saving, setSaving] = useState(false);
    const [followersModal, setFollowersModal] = useState({ open: false, title: '', users: [] });
    const [posts, setPosts] = useState([]);
    const [uploadingPic, setUploadingPic] = useState(false);
    const [followingLoading, setFollowingLoading] = useState(false);
    const fileInputRef = useRef(null);
    const toast = useToast();

    const isOwnProfile = !userId || (profile && (profile.userId === currentUser?.id || profile.userId === currentUser?.userId));

    useEffect(() => {
        const fetchProfileData = async () => {
            setLoading(true);
            try {
                const profileRes = userId ? await getUserProfileById(userId) : await getProfile();
                const profileData = profileRes.data;
                setProfile(profileData);

                if (!userId) {
                    setForm({
                        firstName: profileData.firstName || '',
                        lastName: profileData.lastName || '',
                        bio: profileData.bio || '',
                        city: profileData.city || '',
                    });
                }

                // If we can view posts, fetch them
                if (profileData.canViewPosts) {
                    const postsRes = await getPostsByUser(profileData.userId, 0, 50);
                    setPosts(postsRes.data?.content || []);
                } else {
                    setPosts([]);
                }
            } catch (err) {
                console.error(err);
                const msg = err.response?.data?.message || err.response?.data?.error || 'Failed to load profile';
                toast.error(msg);
                if (userId) navigate('/');
            } finally {
                setLoading(false);
            }
        };

        fetchProfileData();
    }, [userId, navigate]);

    const handleSave = async () => {
        setSaving(true);
        try {
            const res = await updateProfile(form);
            setProfile(res.data);
            setEditing(false);
            toast.success('Profile updated!');
        } catch (err) {
            toast.error(err.response?.data?.message || 'Failed to update');
        } finally {
            setSaving(false);
        }
    };

    const handleFollowToggle = async () => {
        if (!profile || followingLoading) return;
        setFollowingLoading(true);
        try {
            await followUser(profile.userId);
            // Refresh profile to see updated status
            const res = await getUserProfileById(profile.userId);
            setProfile(res.data);
            if (res.data.canViewPosts) {
                const postsRes = await getPostsByUser(profile.userId, 0, 50);
                setPosts(postsRes.data?.content || []);
            }
            toast.success(res.data.isFollowing ? 'Followed!' : res.data.isFollowRequested ? 'Follow request sent!' : 'Unfollowed!');
        } catch (err) {
            toast.error('Failed to update follow status');
        } finally {
            setFollowingLoading(false);
        }
    };

    const handleProfilePicUpload = async (e) => {
        const file = e.target.files?.[0];
        if (!file) return;
        if (file.size > 5 * 1024 * 1024) {
            toast.error('Image must be under 5MB');
            return;
        }
        setUploadingPic(true);
        try {
            const res = await uploadProfilePic(file);
            setProfile(prev => ({ ...prev, profilePicUrl: res.data.profilePicUrl || res.data.imageUrl }));
            toast.success('Profile picture updated!');
        } catch (err) {
            toast.error('Failed to upload profile picture');
        } finally {
            setUploadingPic(false);
        }
    };

    const handleRemoveProfilePic = async () => {
        if (!window.confirm('Remove profile picture?')) return;
        setUploadingPic(true);
        try {
            await removeProfilePicture();
            setProfile(prev => ({ ...prev, profilePicUrl: null }));
            toast.success('Profile picture removed');
        } catch (err) {
            toast.error('Failed to remove profile picture');
        } finally {
            setUploadingPic(false);
        }
    };

    const showFollowers = async () => {
        if (profile?.isPrivateAccount && !profile?.isFollowing && !isOwnProfile) return;
        try {
            const res = await getFollowers(profile?.userId || 0);
            setFollowersModal({ open: true, title: 'Followers', users: Array.isArray(res.data) ? res.data : [] });
        } catch {
            setFollowersModal({ open: true, title: 'Followers', users: [] });
        }
    };

    const showFollowing = async () => {
        if (profile?.isPrivateAccount && !profile?.isFollowing && !isOwnProfile) return;
        try {
            const res = await getFollowing(profile?.userId || 0);
            setFollowersModal({ open: true, title: 'Following', users: Array.isArray(res.data) ? res.data : [] });
        } catch {
            setFollowersModal({ open: true, title: 'Following', users: [] });
        }
    };

    const initial = profile?.firstName?.charAt(0)?.toUpperCase() || profile?.email?.charAt(0)?.toUpperCase() || '?';
    const displayName = [profile?.firstName, profile?.lastName].filter(Boolean).join(' ') || profile?.email?.split('@')[0];

    if (loading) {
        return (
            <div className="max-w-[935px] mx-auto p-4">
                <div className="flex gap-8 items-start mb-8">
                    <div className="w-[120px] h-[120px] sm:w-[150px] sm:h-[150px] rounded-full skeleton flex-shrink-0" />
                    <div className="flex-1 pt-4 space-y-3">
                        <div className="h-6 w-44 skeleton" />
                        <div className="flex gap-8">
                            <div className="h-5 w-16 skeleton" />
                            <div className="h-5 w-16 skeleton" />
                            <div className="h-5 w-16 skeleton" />
                        </div>
                        <div className="h-4 w-32 skeleton" />
                        <div className="h-3 w-64 skeleton" />
                    </div>
                </div>
            </div>
        );
    }

    return (
        <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="max-w-[935px] mx-auto">
            {/* Profile Header */}
            <div className="flex flex-col sm:flex-row gap-6 sm:gap-12 items-center sm:items-start px-4 py-6 sm:py-10">
                {/* Profile Picture */}
                <div className="relative flex-shrink-0">
                    <div className="avatar-story">
                        <div className="avatar w-[100px] h-[100px] sm:w-[150px] sm:h-[150px] text-[32px] sm:text-[42px]">
                            {profile?.profilePicUrl ? (
                                <img src={profile.profilePicUrl} alt={displayName} className="w-full h-full object-cover rounded-full" />
                            ) : initial}
                        </div>
                    </div>
                    {isOwnProfile && (
                        <div className="absolute bottom-1 right-1 flex gap-1">
                            <button
                                onClick={() => fileInputRef.current?.click()}
                                className="w-8 h-8 rounded-full bg-[var(--accent)] flex items-center justify-center cursor-pointer hover:bg-[var(--accent-hover)] transition-colors shadow-lg"
                                title="Upload Picture"
                                disabled={uploadingPic}
                            >
                                {uploadingPic ? <Loader size={14} className="text-white animate-spin" /> : <Camera size={14} className="text-white" />}
                            </button>
                            {profile?.profilePicUrl && (
                                <button
                                    onClick={handleRemoveProfilePic}
                                    className="w-8 h-8 rounded-full bg-[var(--danger)]/80 flex items-center justify-center cursor-pointer hover:bg-[var(--danger)] transition-colors shadow-lg"
                                    title="Remove Picture"
                                    disabled={uploadingPic}
                                >
                                    <UserX size={14} className="text-white" />
                                </button>
                            )}
                        </div>
                    )}
                    <input
                        ref={fileInputRef}
                        type="file"
                        accept="image/jpeg,image/png,image/webp"
                        className="hidden"
                        onChange={handleProfilePicUpload}
                    />
                </div>

                {/* Info */}
                <div className="flex-1 text-center sm:text-left">
                    <div className="flex flex-col sm:flex-row sm:items-center gap-3 mb-5">
                        <h1 className="text-xl font-light truncate max-w-[300px]">{displayName}</h1>
                        <div className="flex gap-2 justify-center sm:justify-start">
                            {isOwnProfile ? (
                                <>
                                    <button
                                        onClick={() => editing ? handleSave() : setEditing(true)}
                                        disabled={saving}
                                        className="px-5 py-1.5 bg-[var(--bg-elevated)] hover:bg-[var(--bg-hover)] text-[var(--text-primary)] text-[13px] font-semibold rounded-lg transition-colors cursor-pointer"
                                    >
                                        {editing ? (saving ? 'Saving...' : 'Save Profile') : 'Edit Profile'}
                                    </button>
                                    <button onClick={() => navigate('/settings')} className="px-2 py-1.5 bg-[var(--bg-elevated)] hover:bg-[var(--bg-hover)] rounded-lg transition-colors cursor-pointer">
                                        <Settings size={16} className="text-[var(--text-primary)]" />
                                    </button>
                                </>
                            ) : (
                                <div className="flex gap-2">
                                    <button
                                        onClick={handleFollowToggle}
                                        disabled={followingLoading}
                                        className={`px-6 py-1.5 rounded-lg text-[13px] font-semibold transition-all flex items-center gap-2 ${profile.isFollowing
                                            ? 'bg-[var(--bg-elevated)] text-[var(--text-primary)] hover:bg-[var(--danger)]/10 hover:text-[var(--danger)]'
                                            : 'bg-[var(--accent)] text-white hover:bg-[var(--accent-hover)]'
                                            }`}
                                    >
                                        {followingLoading ? <Loader size={14} className="animate-spin" /> : (
                                            profile.isFollowing ? <><UserCheck size={14} /> Following</> :
                                                profile.isFollowRequested ? <><Loader size={14} className="animate-pulse" /> Requested</> :
                                                    <><UserPlus size={14} /> Follow</>
                                        )}
                                    </button>
                                    <button
                                        onClick={() => navigate('/chat')}
                                        className="px-6 py-1.5 bg-[var(--bg-elevated)] hover:bg-[var(--bg-hover)] text-[var(--text-primary)] text-[13px] font-semibold rounded-lg transition-colors"
                                    >
                                        Message
                                    </button>
                                </div>
                            )}
                        </div>
                    </div>

                    {/* Stats */}
                    <div className="flex gap-8 mb-4 justify-center sm:justify-start">
                        <StatItem count={posts.length} label="posts" />
                        <StatItem count={profile?.followerCount || 0} label="followers" onClick={showFollowers} />
                        <StatItem count={profile?.followingCount || 0} label="following" onClick={showFollowing} />
                    </div>

                    {/* Bio */}
                    {editing ? (
                        <div className="space-y-2 max-w-sm mx-auto sm:mx-0">
                            <div className="grid grid-cols-2 gap-2">
                                <input className="input-field text-[13px]" placeholder="First Name" value={form.firstName}
                                    onChange={(e) => setForm({ ...form, firstName: e.target.value })} />
                                <input className="input-field text-[13px]" placeholder="Last Name" value={form.lastName}
                                    onChange={(e) => setForm({ ...form, lastName: e.target.value })} />
                            </div>
                            <textarea className="input-field resize-none min-h-[60px] text-[13px]" placeholder="Bio" value={form.bio}
                                onChange={(e) => setForm({ ...form, bio: e.target.value })} />
                            <input className="input-field text-[13px]" placeholder="City" value={form.city}
                                onChange={(e) => setForm({ ...form, city: e.target.value })} />
                            <button onClick={() => setEditing(false)} className="text-[13px] text-[var(--text-muted)] hover:text-[var(--text-secondary)] cursor-pointer">
                                Cancel
                            </button>
                        </div>
                    ) : (
                        <div>
                            <p className="text-[14px] font-semibold text-[var(--text-primary)]">{displayName}</p>
                            {profile?.bio && (
                                <p className="text-[14px] text-[var(--text-secondary)] mt-0.5 whitespace-pre-wrap">{profile.bio}</p>
                            )}
                            <div className="flex flex-wrap gap-3 mt-1 text-[13px] text-[var(--text-muted)] justify-center sm:justify-start">
                                {profile?.city && (
                                    <span className="flex items-center gap-1"><MapPin size={12} /> {profile.city}</span>
                                )}
                                <span className="flex items-center gap-1"><Calendar size={12} /> Joined {profile?.createdAt ? new Date(profile.createdAt).toLocaleDateString('en-US', { month: 'long', year: 'numeric' }) : 'recently'}</span>
                            </div>
                        </div>
                    )}
                </div>
            </div>

            {/* Content Area */}
            <div className="border-t border-[var(--border-color)]">
                {profile?.canViewPosts ? (
                    <>
                        <div className="flex items-center justify-center gap-1.5 py-3 text-[12px] font-semibold text-[var(--text-primary)] tracking-wider uppercase">
                            <Grid3X3 size={12} />
                            <span>Posts</span>
                        </div>

                        {posts.length === 0 ? (
                            <div className="text-center py-16 px-4">
                                <div className="w-16 h-16 rounded-full border border-[var(--border-color)] flex items-center justify-center mx-auto mb-3">
                                    <Camera size={30} strokeWidth={1} className="text-[var(--text-muted)]" />
                                </div>
                                <p className="text-xl font-medium text-[var(--text-primary)] mb-1">No Posts Yet</p>
                                <p className="text-[13px] text-[var(--text-muted)] max-w-xs mx-auto">When they share photos or videos, they'll appear here.</p>
                            </div>
                        ) : (
                            <div className="grid grid-cols-3 gap-[3px] sm:gap-6 px-[1px] auto-rows-fr">
                                {posts.map((post) => (
                                    <motion.div
                                        key={post.id}
                                        initial={{ opacity: 0 }}
                                        animate={{ opacity: 1 }}
                                        whileHover={{ opacity: 0.9 }}
                                        className="relative aspect-square bg-[var(--bg-elevated)] cursor-pointer group overflow-hidden sm:rounded-lg border border-[var(--border-color)]/30"
                                    >
                                        {post.imageUrl ? (
                                            <img src={post.imageUrl} alt="" className="w-full h-full object-cover" />
                                        ) : (
                                            <div className="w-full h-full flex items-center justify-center p-3 sm:p-5">
                                                <p className="text-[10px] sm:text-[13px] text-[var(--text-secondary)] text-center line-clamp-6 leading-relaxed italic">{post.content}</p>
                                            </div>
                                        )}
                                        <div className="absolute inset-0 bg-black/40 opacity-0 group-hover:opacity-100 transition-all duration-300 flex items-center justify-center gap-4 sm:gap-8">
                                            <span className="text-white text-[13px] sm:text-[16px] font-bold flex items-center gap-1.5">❤️ {post.likeCount || 0}</span>
                                            <span className="text-white text-[13px] sm:text-[16px] font-bold flex items-center gap-1.5">💬 {post.commentCount || 0}</span>
                                        </div>
                                    </motion.div>
                                ))}
                            </div>
                        )}
                    </>
                ) : (
                    <div className="flex flex-col items-center justify-center py-20 px-8 text-center bg-[var(--bg-card)]/30 rounded-2xl mx-4 mt-6 border border-dashed border-[var(--border-color)]">
                        <div className="w-20 h-20 rounded-full border-2 border-[var(--text-primary)] flex items-center justify-center mb-6">
                            <Lock size={40} className="text-[var(--text-primary)]" />
                        </div>
                        <h3 className="text-[16px] font-bold text-[var(--text-primary)] mb-1">This account is private</h3>
                        <p className="text-[13px] text-[var(--text-muted)] max-w-[260px]">Follow this account to see their photos and videos.</p>

                        {!profile.isFollowRequested && (
                            <button
                                onClick={handleFollowToggle}
                                className="mt-6 text-[14px] font-bold text-[var(--accent)] hover:text-[var(--accent-hover)] transition-colors px-6 py-2 rounded-full border border-[var(--accent)]/30 hover:bg-[var(--accent-light)]"
                            >
                                Request to follow
                            </button>
                        )}
                    </div>
                )}
            </div>

            <FollowersModal
                open={followersModal.open}
                onClose={() => setFollowersModal({ ...followersModal, open: false })}
                title={followersModal.title}
                users={followersModal.users}
            />
        </motion.div>
    );
}
