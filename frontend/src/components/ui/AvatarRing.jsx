import { motion } from 'framer-motion';

export default function AvatarRing({ src, size = 40, alt, isActive = false }) {
    return (
        <div className="relative inline-block">
            {isActive && (
                <motion.div
                    animate={{ rotate: 360 }}
                    transition={{ duration: 8, repeat: Infinity, ease: "linear" }}
                    className="absolute inset-0 rounded-full bg-gradient-to-tr from-[var(--gradient-1)] via-[var(--gradient-2)] to-[var(--gradient-3)]"
                    style={{ padding: '2px', margin: '-2px' }}
                />
            )}
            <div
                className={`relative rounded-full overflow-hidden bg-[var(--bg-elevated)] border-2 border-[var(--bg-primary)]`}
                style={{ width: size, height: size }}
            >
                {src ? (
                    <img src={src} alt={alt || "Avatar"} className="w-full h-full object-cover" />
                ) : (
                    <div className="w-full h-full flex items-center justify-center text-[var(--text-muted)] font-bold text-xs">
                        {alt?.charAt(0).toUpperCase() || "?"}
                    </div>
                )}
            </div>
        </div>
    );
}
