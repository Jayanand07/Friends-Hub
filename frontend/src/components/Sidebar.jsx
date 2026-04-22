import { NavLink, useNavigate } from 'react-router-dom';
import { Home, Search, MessageCircle, Settings, Users, LogOut, PlusSquare } from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { useEffect, useState } from 'react';
import { getProfile } from '../api/users';

const links = [
    { to: '/', icon: Home, label: 'Home' },
    { to: '/search', icon: Search, label: 'Search' },
    { to: '/chat', icon: MessageCircle, label: 'Messages' },
];

export default function Sidebar({ onCreatePost }) {
    const { user, logout } = useAuth();
    const navigate = useNavigate();
    const [profile, setProfile] = useState(null);

    useEffect(() => {
        getProfile().then(res => setProfile(res.data)).catch(() => { });
    }, []);

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
                <div onClick={() => navigate('/')} className="cursor-pointer hover:opacity-80 transition-opacity" style={{ display:'flex', alignItems:'center', gap:'12px', padding:'4px 0' }}>
                    <div style={{
                        width:'42px', height:'42px', borderRadius:'13px',
                        background:'#E91E8C', position:'relative', overflow:'hidden', flexShrink:0,
                        boxShadow:'0 4px 16px rgba(233,30,140,0.35)'
                    }}>
                        <div style={{
                            position:'absolute', width:'26px', height:'23px',
                            background:'#000000', borderRadius:'6px 6px 6px 2px',
                            top:'8px', left:'7px'
                        }}/>
                        <div style={{
                            position:'absolute', width:'26px', height:'23px',
                            background:'rgba(0,0,0,0.3)', borderRadius:'6px 6px 2px 6px',
                            bottom:'8px', right:'7px'
                        }}/>
                    </div>
                    <span style={{
                        fontFamily:'Syne, sans-serif', fontWeight:800,
                        fontSize:'22px', letterSpacing:'-0.02em', lineHeight:1
                    }}>
                        <span style={{color:'#FFFFFF'}}>Friends</span>
                        <span style={{color:'#E91E8C'}}>Hub</span>
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
                                <div className="relative z-10 flex items-center gap-3.5">
                                    <Icon size={24} strokeWidth={isActive ? 2.5 : 1.5} className={`transition-transform duration-300 ${isActive ? 'scale-110' : 'group-hover:scale-110'}`} />
                                    <span className="hidden md:block lg:block">{label}</span>
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
                    className="flex items-center gap-3.5 px-4 py-3 rounded-xl text-[15px] font-medium text-white bg-gradient-to-r from-[var(--gradient-1)] to-[var(--gradient-2)] hover:opacity-90 hover:scale-[1.02] active:scale-[0.98] transition-all duration-200 cursor-pointer shadow-lg shadow-[var(--accent-glow)] mt-1"
                >
                    <PlusSquare size={24} strokeWidth={2} />
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
                                <div className={`w-6 h-6 rounded-full overflow-hidden flex items-center justify-center text-[9px] font-bold ${isActive ? 'ring-2 ring-white/50' : ''}`}
                                    style={{ background: profilePic ? 'transparent' : 'linear-gradient(135deg, #833ab4, #fd1d1d, #fcb045)' }}>
                                    {profilePic ? (
                                        <img src={profilePic} alt="Profile" className="w-full h-full object-cover" />
                                    ) : (
                                        <span className="text-white">{initial}</span>
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
            <div className="mt-auto">
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
