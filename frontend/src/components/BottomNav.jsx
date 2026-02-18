import { NavLink } from 'react-router-dom';
import { Home, Search, PlusSquare, Heart, User } from 'lucide-react';

export default function BottomNav({ onCreatePost }) {
    return (
        <nav className="bottom-nav lg:hidden glass-strong pb-[env(safe-area-inset-bottom)]">
            <div className="flex items-center justify-between max-w-sm mx-auto relative px-6">
                <NavLink to="/" className={({ isActive }) =>
                    `flex flex-col items-center py-1.5 px-3 transition-colors ${isActive ? 'text-[var(--text-primary)]' : 'text-[var(--text-muted)]'}`}>
                    {({ isActive }) => <Home size={24} strokeWidth={isActive ? 2.5 : 1.5} />}
                </NavLink>

                <NavLink to="/search" className={({ isActive }) =>
                    `flex flex-col items-center py-1.5 px-3 transition-colors ${isActive ? 'text-[var(--text-primary)]' : 'text-[var(--text-muted)]'}`}>
                    {({ isActive }) => <Search size={24} strokeWidth={isActive ? 2.5 : 1.5} />}
                </NavLink>

                <div className="relative -top-5">
                    <button
                        onClick={onCreatePost}
                        className="flex items-center justify-center w-12 h-12 bg-gradient-to-tr from-[var(--gradient-1)] to-[var(--gradient-2)] rounded-full text-white shadow-lg shadow-[var(--accent-glow)] hover:scale-105 active:scale-95 transition-transform duration-200 border-4 border-[var(--bg-primary)]"
                    >
                        <PlusSquare size={24} strokeWidth={2} />
                    </button>
                </div>

                <NavLink to="/notifications" className={({ isActive }) =>
                    `flex flex-col items-center py-1.5 px-3 transition-colors ${isActive ? 'text-[var(--text-primary)]' : 'text-[var(--text-muted)]'}`}>
                    {({ isActive }) => <Heart size={24} strokeWidth={isActive ? 2.5 : 1.5} />}
                </NavLink>

                <NavLink to="/profile" className={({ isActive }) =>
                    `flex flex-col items-center py-1.5 px-3 transition-colors ${isActive ? 'text-[var(--text-primary)]' : 'text-[var(--text-muted)]'}`}>
                    {({ isActive }) => (
                        <div className={`w-6 h-6 rounded-full flex items-center justify-center text-[9px] font-bold ${isActive ? 'ring-1 ring-[var(--text-primary)]' : ''}`}
                            style={{ background: 'linear-gradient(135deg, #833ab4, #fd1d1d, #fcb045)' }}>
                            <span className="text-white">?</span>
                        </div>
                    )}
                </NavLink>
            </div>
        </nav>
    );
}
