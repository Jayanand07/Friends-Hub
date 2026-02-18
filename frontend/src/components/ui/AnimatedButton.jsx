import { motion } from 'framer-motion';

export default function AnimatedButton({ children, onClick, variant = 'primary', className = '', disabled = false, icon: Icon }) {
    const baseStyles = "relative overflow-hidden rounded-xl font-semibold transition-all duration-300 flex items-center justify-center gap-2";

    const variants = {
        primary: "bg-gradient-to-r from-[var(--gradient-1)] via-[var(--gradient-2)] to-[var(--gradient-3)] text-white shadow-lg shadow-[var(--accent-glow)] hover:shadow-xl hover:shadow-[var(--accent)]/20",
        secondary: "bg-[var(--bg-elevated)] text-[var(--text-primary)] border border-[var(--border-color)] hover:border-[var(--text-muted)] hover:bg-[var(--bg-hover)]",
        ghost: "bg-transparent text-[var(--text-secondary)] hover:text-[var(--text-primary)] hover:bg-[var(--bg-elevated)]/50",
        danger: "bg-[var(--danger)]/10 text-[var(--danger)] hover:bg-[var(--danger)]/20 border border-[var(--danger)]/20"
    };

    return (
        <motion.button
            whileHover={{ scale: disabled ? 1 : 1.02 }}
            whileTap={{ scale: disabled ? 1 : 0.98 }}
            onClick={onClick}
            disabled={disabled}
            className={`${baseStyles} ${variants[variant]} ${disabled ? 'opacity-50 cursor-not-allowed grayscale' : 'cursor-pointer'} ${className} px-5 py-2.5`}
        >
            {Icon && <Icon size={18} strokeWidth={2.5} />}
            <span className="relative z-10">{children}</span>
        </motion.button>
    );
}
