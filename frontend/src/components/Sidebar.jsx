import { NavLink, useNavigate, useLocation } from 'react-router-dom';
import { Home, Search, MessageCircle, Settings, Users, LogOut, PlusSquare, Moon, Sun, Activity } from 'lucide-react';
import { useTheme } from '../context/ThemeContext';
import { useAuth } from '../context/AuthContext';
import { useEffect, useState } from 'react';
import { getProfile } from '../api/users';

const links = [
    { to: '/', icon: Home, label: 'Home' },
    { to: '/search', icon: Search, label: 'Search' },
    { to: '/activity', icon: Activity, label: 'Activity' },
    { to: '/chat', icon: MessageCircle, label: 'Messages' },
];

export default function Sidebar({ onCreatePost, isCreateOpen }) {
    const { user, logout } = useAuth();
    const { isDarkMode, toggleTheme } = useTheme();
    const navigate = useNavigate();
    const location = useLocation();
    const [profile, setProfile] = useState(null);
    const [hasUnreadMsg, setHasUnreadMsg] = useState(false);

    useEffect(() => {
        getProfile().then(res => setProfile(res.data)).catch(() => { });
    }, []);

    useEffect(() => {
        const handleUnread = () => {
            if (!location.pathname.startsWith('/chat')) {
                setHasUnreadMsg(true);
            }
        };
        const handleRead = () => setHasUnreadMsg(false);
        window.addEventListener('unreadChatMessage', handleUnread);
        window.addEventListener('chatMessagesRead', handleRead);
        return () => {
            window.removeEventListener('unreadChatMessage', handleUnread);
            window.removeEventListener('chatMessagesRead', handleRead);
        };
    }, []);

    useEffect(() => {
        if (location.pathname.startsWith('/chat')) {
            setHasUnreadMsg(false);
        }
    }, [location.pathname]);

    const handleLogout = () => {
        logout();
        navigate('/login');
    };

    const initial = profile?.firstName?.charAt(0)?.toUpperCase() || user?.email?.charAt(0)?.toUpperCase() || '?';
    const profilePic = profile?.profilePicUrl;

    return (
        <aside className="hidden lg:flex flex-col w-[240px] h-screen sticky top-0 border-r border-[var(--border-color)] glass z-50 px-4 py-6 justify-between transition-colors duration-300">
            {/* Logo */}
            <div className="px-2 mb-8">
                <div onClick={() => navigate('/')} className="cursor-pointer hover:opacity-80 transition-opacity flex items-center px-2 py-2">
                    <span style={{
                        fontFamily: "'Grand Hotel', cursive",
                        fontSize: '36px', 
                        lineHeight: 1
                    }}>
                        <span style={{color:'var(--text-primary)'}}>Friends</span>
                        <span className="text-[var(--accent)]">Hub</span>
                    </span>
                </div>
            </div>

            {/* Nav Links */}
            <nav className="flex flex-col gap-2 flex-1">
                {links.map(({ to, icon: Icon, label }) => (
                    <NavLink
                        key={to}
                        to={to}
                        className={({ isActive }) =>
                            `flex items-center gap-3.5 px-4 py-3 rounded-xl text-[15px] transition-all duration-300 group relative overflow-hidden ${isActive
                                ? 'font-bold text-white shadow-lg shadow-[var(--accent-glow)]'
                                : 'font-normal text-[var(--text-secondary)] hover:bg-[var(--bg-elevated)] hover:text-[var(--text-primary)] hover:translate-x-1'
                            }`
                        }
                    >
                        {({ isActive }) => (
                            <>
                                {isActive && (
                                    <div className="absolute inset-0 bg-gradient-to-r from-[var(--gradient-1)] to-[var(--gradient-2)] opacity-100 z-0" />
                                )}
                                <div className="relative z-10 flex items-center gap-3.5 w-full">
                                    <div className="relative">
                                        <Icon size={24} strokeWidth={isActive ? 2.5 : 1.5} className={`transition-transform duration-300 ${isActive ? 'scale-110' : 'group-hover:scale-110'}`} />
                                        {to === '/chat' && hasUnreadMsg && !isActive && (
                                            <span className="absolute -top-1 -right-1 w-2.5 h-2.5 bg-red-500 rounded-full border-2 border-[var(--bg-card)] animate-ping" />
                                        )}
                                        {to === '/chat' && hasUnreadMsg && !isActive && (
                                            <span className="absolute -top-1 -right-1 w-2.5 h-2.5 bg-red-500 rounded-full border-2 border-[var(--bg-card)] shadow-md shadow-red-500/50" />
                                        )}
                                    </div>
                                    <span className="hidden md:block lg:block flex-1">{label}</span>
                                    {to === '/chat' && hasUnreadMsg && !isActive && (
                                        <span className="text-[10px] font-bold bg-red-500 text-white px-2 py-0.5 rounded-full shadow-sm">NEW</span>
                                    )}
                                </div>
                            </>
                        )}
                    </NavLink>
                ))}

                {/* Groups link */}
                <NavLink
                    to="/groups"
                    className={({ isActive }) =>
                        `flex items-center gap-3.5 px-4 py-3 rounded-xl text-[15px] transition-all duration-300 group relative overflow-hidden ${isActive
                            ? 'font-bold text-white shadow-lg shadow-[var(--accent-glow)]'
                            : 'font-normal text-[var(--text-secondary)] hover:bg-[var(--bg-elevated)] hover:text-[var(--text-primary)] hover:translate-x-1'
                        }`
                    }
                >
                    {({ isActive }) => (
                        <>
                            {isActive && (
                                <div className="absolute inset-0 bg-gradient-to-r from-[var(--gradient-1)] to-[var(--gradient-2)] opacity-100 z-0" />
                            )}
                            <div className="relative z-10 flex items-center gap-3.5">
                                <Users size={24} strokeWidth={isActive ? 2.5 : 1.5} className={`transition-transform duration-300 ${isActive ? 'scale-110' : 'group-hover:scale-110'}`} />
                                <span className="hidden md:block lg:block">Groups</span>
                            </div>
                        </>
                    )}
                </NavLink>

                {/* Create Post button */}
                <button
                    onClick={onCreatePost}
                    className={`flex items-center gap-3.5 px-4 py-3 rounded-xl text-[15px] transition-all duration-300 group relative overflow-hidden cursor-pointer ${
                        isCreateOpen
                            ? 'font-bold text-white shadow-lg shadow-[var(--accent-glow)] bg-gradient-to-r from-[var(--gradient-1)] to-[var(--gradient-2)]'
                            : 'font-normal text-[var(--text-secondary)] hover:bg-[var(--bg-elevated)] hover:text-[var(--text-primary)] hover:translate-x-1'
                    }`}
                >
                    <PlusSquare size={24} strokeWidth={isCreateOpen ? 2.5 : 1.5} className={`transition-transform duration-300 ${isCreateOpen ? 'scale-110' : 'group-hover:scale-110'}`} />
                    <span className="hidden md:block lg:block">Create</span>
                </button>

                {/* Profile link */}
                <NavLink
                    to="/profile"
                    className={({ isActive }) =>
                        `flex items-center gap-3.5 px-4 py-3 rounded-xl text-[15px] transition-all duration-300 group relative overflow-hidden ${isActive
                            ? 'font-bold text-white shadow-lg shadow-[var(--accent-glow)]'
                            : 'font-normal text-[var(--text-secondary)] hover:bg-[var(--bg-elevated)] hover:text-[var(--text-primary)] hover:translate-x-1'
                        }`
                    }
                >
                    {({ isActive }) => (
                        <>
                            {isActive && (
                                <div className="absolute inset-0 bg-gradient-to-r from-[var(--gradient-1)] to-[var(--gradient-2)] opacity-100 z-0" />
                            )}
                            <div className="relative z-10 flex items-center gap-3.5">
                                <div className={`w-6 h-6 rounded-full overflow-hidden flex items-center justify-center text-[10px] font-bold bg-[var(--accent)] text-white ${isActive ? 'ring-2 ring-white/50' : ''}`}>
                                    {profilePic ? (
                                        <img src={profilePic} alt="Profile" className="w-full h-full object-cover" />
                                    ) : (
                                        <span>{initial}</span>
                                    )}
                                </div>
                                <span className="hidden md:block lg:block">Profile</span>
                            </div>
                        </>
                    )}
                </NavLink>

                {/* Settings link */}
                <NavLink
                    to="/settings"
                    className={({ isActive }) =>
                        `flex items-center gap-3.5 px-4 py-3 rounded-xl text-[15px] transition-all duration-300 group relative overflow-hidden ${isActive
                            ? 'font-bold text-white shadow-lg shadow-[var(--accent-glow)]'
                            : 'font-normal text-[var(--text-secondary)] hover:bg-[var(--bg-elevated)] hover:text-[var(--text-primary)] hover:translate-x-1'
                        }`
                    }
                >
                    {({ isActive }) => (
                        <>
                            {isActive && (
                                <div className="absolute inset-0 bg-gradient-to-r from-[var(--gradient-1)] to-[var(--gradient-2)] opacity-100 z-0" />
                            )}
                            <div className="relative z-10 flex items-center gap-3.5">
                                <Settings size={24} strokeWidth={isActive ? 2.5 : 1.5} className={`transition-transform duration-300 ${isActive ? 'scale-110' : 'group-hover:scale-110'}`} />
                                <span className="hidden md:block lg:block">Settings</span>
                            </div>
                        </>
                    )}
                </NavLink>
            </nav>

            {/* Footer */}
            <div className="px-4 mt-6 pt-6 border-t border-[var(--border-color)] space-y-2">
                {/* Theme Toggle */}
                <button
                    onClick={toggleTheme}
                    className="flex items-center gap-4 w-full px-4 py-3 text-[var(--text-secondary)] hover:bg-[var(--bg-elevated)] hover:text-[var(--text-primary)] rounded-xl transition-all group"
                >
                    <div className="relative group-hover:scale-110 transition-transform duration-300">
                        {isDarkMode ? <Sun size={22} className="text-[#fdcb6e]" /> : <Moon size={22} className="text-[var(--accent)]" />}
                    </div>
                    <span className="font-semibold text-[15px]">{isDarkMode ? 'Light Mode' : 'Dark Mode'}</span>
                </button>
                
                <button
                    onClick={handleLogout}
                    className="flex items-center gap-3.5 px-4 py-3 rounded-xl text-[15px] text-[var(--text-secondary)] hover:bg-[var(--bg-elevated)] hover:text-[var(--text-primary)] transition-all w-full cursor-pointer hover:translate-x-1"
                >
                    <LogOut size={24} strokeWidth={1.5} />
                    <span>Logout</span>
                </button>
            </div>
        </aside>
    );
}
