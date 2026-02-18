import { createContext, useContext, useState, useCallback } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { CheckCircle, XCircle, Info, X } from 'lucide-react';

const ToastContext = createContext(null);

let toastId = 0;

export function ToastProvider({ children }) {
    const [toasts, setToasts] = useState([]);

    const addToast = useCallback((message, type = 'success') => {
        const id = ++toastId;
        setToasts((prev) => [...prev, { id, message, type }]);
        setTimeout(() => {
            setToasts((prev) => prev.filter((t) => t.id !== id));
        }, 4000);
    }, []);

    const removeToast = useCallback((id) => {
        setToasts((prev) => prev.filter((t) => t.id !== id));
    }, []);

    const toast = {
        success: (msg) => addToast(msg, 'success'),
        error: (msg) => addToast(msg, 'error'),
        info: (msg) => addToast(msg, 'info'),
    };

    const icons = {
        success: <CheckCircle size={18} className="text-emerald-400 flex-shrink-0" />,
        error: <XCircle size={18} className="text-red-400 flex-shrink-0" />,
        info: <Info size={18} className="text-blue-400 flex-shrink-0" />,
    };

    const borderColors = {
        success: 'border-emerald-500/30',
        error: 'border-red-500/30',
        info: 'border-blue-500/30',
    };

    return (
        <ToastContext.Provider value={toast}>
            {children}
            <div className="fixed bottom-6 right-6 z-[100] flex flex-col gap-2 max-w-sm">
                <AnimatePresence>
                    {toasts.map((t) => (
                        <motion.div
                            key={t.id}
                            initial={{ opacity: 0, y: 20, scale: 0.95 }}
                            animate={{ opacity: 1, y: 0, scale: 1 }}
                            exit={{ opacity: 0, x: 80, scale: 0.95 }}
                            className={`glass-strong rounded-xl px-4 py-3 flex items-center gap-3 border ${borderColors[t.type]}`}
                        >
                            {icons[t.type]}
                            <span className="text-sm text-[var(--text-primary)] flex-1">{t.message}</span>
                            <button
                                onClick={() => removeToast(t.id)}
                                className="p-1 rounded-lg hover:bg-[var(--bg-elevated)] transition-colors cursor-pointer flex-shrink-0"
                            >
                                <X size={14} className="text-[var(--text-muted)]" />
                            </button>
                        </motion.div>
                    ))}
                </AnimatePresence>
            </div>
        </ToastContext.Provider>
    );
}

export const useToast = () => useContext(ToastContext);
