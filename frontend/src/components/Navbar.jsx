import { useAuth } from '../context/AuthContext';
import { Home, User, LogOut, Menu, X, Search, MessageCircle } from 'lucide-react';
import { NavLink, useNavigate } from 'react-router-dom';
import { useState, useRef, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import NotificationBell from './NotificationBell';

export default function Navbar({ newNotification }) {
    const { user, logout } = useAuth();
    const navigate = useNavigate();
    const [menuOpen, setMenuOpen] = useState(false);
    const [dropdownOpen, setDropdownOpen] = useState(false);
    const dropdownRef = useRef(null);

    const handleLogout = () => {
        logout();
        navigate('/login');
    };

    useEffect(() => {
        const handler = (e) => {
            if (dropdownRef.current && !dropdownRef.current.contains(e.target)) {
                setDropdownOpen(false);
            }
        };
        document.addEventListener('mousedown', handler);
        return () => document.removeEventListener('mousedown', handler);
    }, []);

    const initial = user?.email?.charAt(0)?.toUpperCase() || '?';

    return (
        <>
            <header className="sticky top-0 z-40 glass border-b border-[var(--border-color)] transition-all duration-300">
                <div className="max-w-[935px] mx-auto px-4 h-[60px] flex items-center justify-between gap-4">
                    {/* Left — Logo */}
                    <div className="flex items-center gap-3 lg:hidden">
                        <div style={{ display:'flex', alignItems:'center', gap:'10px' }}>
                          <div style={{
                            width:'34px', height:'34px', borderRadius:'10px',
                            background:'#E91E8C', position:'relative', overflow:'hidden', flexShrink:0,
                            boxShadow:'0 4px 12px rgba(233,30,140,0.35)'
                          }}>
                            <div style={{
                              position:'absolute', width:'21px', height:'18px',
                              background:'#000000', borderRadius:'5px 5px 5px 2px',
                              top:'6px', left:'6px'
                            }}/>
                            <div style={{
                              position:'absolute', width:'21px', height:'18px',
                              background:'rgba(0,0,0,0.3)', borderRadius:'5px 5px 2px 5px',
                              bottom:'6px', right:'6px'
                            }}/>
                          </div>
                          <span style={{
                            fontFamily:'Syne, sans-serif', fontWeight:800,
                            fontSize:'18px', letterSpacing:'-0.02em', lineHeight:1
                          }}>
                            <span style={{color:'#FFFFFF'}}>Friends</span>
                            <span style={{color:'#E91E8C'}}>Hub</span>
                          </span>
                        </div>
                    </div>

                    {/* Center — Search (desktop) */}
                    <div className="hidden md:flex flex-1 max-w-[268px] mx-auto">
                        <div className="relative w-full">
                            <Search size={14} className="absolute left-3 top-1/2 -translate-y-1/2 text-[var(--text-muted)] pointer-events-none" />
                            <input
                                type="text"
                                placeholder="Search"
                                onFocus={() => navigate('/search')}
                                className="w-full pl-9 pr-3 py-2 bg-[var(--bg-elevated)] border border-transparent focus:border-[var(--border-hover)] rounded-full text-[13px] text-[var(--text-primary)] placeholder-[var(--text-muted)] outline-none transition-all duration-200 focus:bg-[var(--bg-primary)] shadow-inner cursor-pointer"
                                readOnly
                            />
                        </div>
                    </div>

                    {/* Right — Actions */}
                    <div className="flex items-center gap-1">
                        <NotificationBell newNotification={newNotification} />

                        {/* Avatar Dropdown */}
                        <div className="relative" ref={dropdownRef}>
                            <button
                                onClick={() => setDropdownOpen(!dropdownOpen)}
                                className="w-7 h-7 rounded-full overflow-hidden flex items-center justify-center cursor-pointer border border-transparent hover:border-[var(--text-muted)] transition-colors"
                                style={{ background: 'linear-gradient(135deg, #833ab4, #fd1d1d, #fcb045)' }}
                            >
                                <span className="text-white text-[10px] font-bold">{initial}</span>
                            </button>

                            <AnimatePresence>
                                {dropdownOpen && (
                                    <motion.div
                                        initial={{ opacity: 0, y: 8, scale: 0.95 }}
                                        animate={{ opacity: 1, y: 0, scale: 1 }}
                                        exit={{ opacity: 0, y: 8, scale: 0.95 }}
                                        className="absolute right-0 mt-2 bg-[var(--bg-elevated)] border border-[var(--border-color)] rounded-xl p-1 min-w-[200px] z-50 shadow-xl"
                                    >
                                        <p className="px-3 py-2 text-[11px] text-[var(--text-muted)] truncate border-b border-[var(--border-color)] mb-1">{user?.email}</p>
                                        <NavLink
                                            to="/profile"
                                            onClick={() => setDropdownOpen(false)}
                                            className="flex items-center gap-2.5 px-3 py-2.5 text-[13px] text-[var(--text-primary)] hover:bg-[var(--bg-hover)] rounded-lg transition-colors"
                                        >
                                            <User size={16} /> Profile
                                        </NavLink>
                                        <NavLink
                                            to="/chat"
                                            onClick={() => setDropdownOpen(false)}
                                            className="flex items-center gap-2.5 px-3 py-2.5 text-[13px] text-[var(--text-primary)] hover:bg-[var(--bg-hover)] rounded-lg transition-colors"
                                        >
                                            <MessageCircle size={16} /> Messages
                                        </NavLink>
                                        <div className="border-t border-[var(--border-color)] mt-1 pt-1">
                                            <button
                                                onClick={handleLogout}
                                                className="w-full flex items-center gap-2.5 px-3 py-2.5 text-[13px] text-[var(--text-primary)] hover:bg-[var(--bg-hover)] rounded-lg transition-colors cursor-pointer"
                                            >
                                                <LogOut size={16} /> Log out
                                            </button>
                                        </div>
                                    </motion.div>
                                )}
                            </AnimatePresence>
                        </div>

                        {/* Mobile hamburger */}
                        <button
                            onClick={() => setMenuOpen(!menuOpen)}
                            className="lg:hidden btn-icon"
                        >
                            {menuOpen ? <X size={22} /> : <Menu size={22} />}
                        </button>
                    </div>
                </div>

                {/* Mobile Nav Drawer */}
                <AnimatePresence>
                    {menuOpen && (
                        <motion.div
                            initial={{ height: 0, opacity: 0 }}
                            animate={{ height: 'auto', opacity: 1 }}
                            exit={{ height: 0, opacity: 0 }}
                            className="lg:hidden border-t border-[var(--border-color)] overflow-hidden bg-[var(--bg-primary)]"
                        >
                            <nav className="p-3 flex flex-col gap-0.5">
                                <div className="relative mb-2 md:hidden">
                                    <Search size={14} className="absolute left-3 top-1/2 -translate-y-1/2 text-[var(--text-muted)]" />
                                    <input
                                        type="text"
                                        placeholder="Search"
                                        onFocus={() => { setMenuOpen(false); navigate('/search'); }}
                                        className="w-full pl-9 pr-3 py-2 bg-[var(--bg-elevated)] border-none rounded-lg text-[13px] text-[var(--text-primary)] placeholder-[var(--text-muted)] outline-none cursor-pointer"
                                        readOnly
                                    />
                                </div>
                                <NavLink to="/" onClick={() => setMenuOpen(false)}
                                    className="flex items-center gap-3 px-3 py-2.5 rounded-lg text-[14px] text-[var(--text-primary)] hover:bg-[var(--bg-elevated)] transition-colors">
                                    <Home size={20} /> Home
                                </NavLink>
                                <NavLink to="/chat" onClick={() => setMenuOpen(false)}
                                    className="flex items-center gap-3 px-3 py-2.5 rounded-lg text-[14px] text-[var(--text-primary)] hover:bg-[var(--bg-elevated)] transition-colors">
                                    <MessageCircle size={20} /> Messages
                                </NavLink>
                                <NavLink to="/profile" onClick={() => setMenuOpen(false)}
                                    className="flex items-center gap-3 px-3 py-2.5 rounded-lg text-[14px] text-[var(--text-primary)] hover:bg-[var(--bg-elevated)] transition-colors">
                                    <User size={20} /> Profile
                                </NavLink>
                                <div className="border-t border-[var(--border-color)] mt-1 pt-1">
                                    <button onClick={handleLogout}
                                        className="flex items-center gap-3 px-3 py-2.5 rounded-lg text-[14px] text-[var(--text-primary)] hover:bg-[var(--bg-elevated)] transition-colors cursor-pointer w-full">
                                        <LogOut size={20} /> Log out
                                    </button>
                                </div>
                            </nav>
                        </motion.div>
                    )}
                </AnimatePresence>
            </header>
        </>
    );
}
