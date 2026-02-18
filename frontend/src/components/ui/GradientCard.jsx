import { motion } from 'framer-motion';

export default function GradientCard({ children, className = '', hoverEffect = true }) {
    return (
        <motion.div
            whileHover={hoverEffect ? { y: -5, boxShadow: "0 20px 40px -15px rgba(0,0,0,0.5)" } : {}}
            transition={{ type: "spring", stiffness: 300, damping: 20 }}
            className={`relative overflow-hidden rounded-2xl bg-[var(--bg-card)] border border-[var(--border-color)] p-6 transition-colors ${className}`}
        >
            {/* Gradient Glow */}
            <div className="absolute -top-20 -right-20 w-40 h-40 bg-[var(--gradient-1)] rounded-full blur-[80px] opacity-10 pointer-events-none group-hover:opacity-20 transition-opacity" />
            <div className="absolute -bottom-20 -left-20 w-40 h-40 bg-[var(--gradient-2)] rounded-full blur-[80px] opacity-5 pointer-events-none group-hover:opacity-15 transition-opacity" />

            <div className="relative z-10">
                {children}
            </div>
        </motion.div>
    );
}
