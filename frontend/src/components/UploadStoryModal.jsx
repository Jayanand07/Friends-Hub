import { useState, useRef, useCallback } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { Plus, Loader } from 'lucide-react';
import { uploadStory } from '../api/stories';
import { useToast } from './Toast';

export default function UploadStoryModal({ open, onClose, onUploaded }) {
    const [preview, setPreview] = useState(null);
    const [file, setFile] = useState(null);
    const [uploading, setUploading] = useState(false);
    const [progress, setProgress] = useState(0);
    const [dragOver, setDragOver] = useState(false);
    const fileRef = useRef(null);
    const toast = useToast();

    const handleFile = (f) => {
        if (!f) return;
        if (f.size > 5 * 1024 * 1024) {
            toast.error('Image must be under 5MB');
            return;
        }
        if (!['image/jpeg', 'image/png', 'image/webp'].includes(f.type)) {
            toast.error('Only JPEG, PNG, WebP allowed');
            return;
        }
        setFile(f);
        const reader = new FileReader();
        reader.onload = (e) => setPreview(e.target.result);
        reader.readAsDataURL(f);
    };

    const handleDrop = useCallback((e) => {
        e.preventDefault();
        setDragOver(false);
        const f = e.dataTransfer.files?.[0];
        handleFile(f);
    }, []);

    const handleUpload = async () => {
        if (!file) return;
        setUploading(true);
        try {
            await uploadStory(file, setProgress);
            toast.success('Story uploaded!');
            onUploaded?.();
            handleClose();
        } catch (err) {
            toast.error('Failed to upload story');
        } finally {
            setUploading(false);
            setProgress(0);
        }
    };

    const handleClose = () => {
        setPreview(null);
        setFile(null);
        setProgress(0);
        onClose();
    };

    if (!open) return null;

    return (
        <AnimatePresence>
            <motion.div
                initial={{ opacity: 0 }}
                animate={{ opacity: 1 }}
                exit={{ opacity: 0 }}
                className="fixed inset-0 z-[60] flex items-center justify-center bg-black/80 backdrop-blur-sm p-4"
                onClick={handleClose}
            >
                <motion.div
                    initial={{ scale: 0.9, opacity: 0 }}
                    animate={{ scale: 1, opacity: 1 }}
                    exit={{ scale: 0.9, opacity: 0 }}
                    transition={{ type: 'spring', damping: 25 }}
                    className="bg-[var(--bg-card)] border border-[var(--border-color)] rounded-xl w-full max-w-md overflow-hidden"
                    onClick={(e) => e.stopPropagation()}
                >
                    {/* Header */}
                    <div className="flex items-center justify-between px-4 py-3 border-b border-[var(--border-color)]">
                        <button onClick={handleClose} className="text-[14px] text-[var(--text-secondary)] cursor-pointer hover:text-[var(--text-primary)]">Cancel</button>
                        <h3 className="text-[15px] font-semibold text-[var(--text-primary)]">New Story</h3>
                        <button
                            onClick={handleUpload}
                            disabled={!file || uploading}
                            className="text-[14px] font-semibold text-[var(--accent)] cursor-pointer disabled:opacity-30 hover:text-[var(--accent-hover)]"
                        >
                            {uploading ? 'Sharing...' : 'Share'}
                        </button>
                    </div>

                    {/* Content */}
                    <div className="p-4">
                        {preview ? (
                            <div className="relative">
                                <img src={preview} alt="Preview" className="w-full max-h-[400px] object-contain rounded-lg" />
                                {uploading && (
                                    <div className="absolute bottom-0 left-0 right-0 p-3">
                                        <div className="h-1 bg-[var(--bg-elevated)] rounded-full overflow-hidden">
                                            <motion.div
                                                className="h-full bg-[var(--accent)] rounded-full"
                                                initial={{ width: 0 }}
                                                animate={{ width: `${progress}%` }}
                                            />
                                        </div>
                                    </div>
                                )}
                                {!uploading && (
                                    <button
                                        onClick={() => { setPreview(null); setFile(null); }}
                                        className="absolute top-2 right-2 w-7 h-7 rounded-full bg-black/60 flex items-center justify-center text-white cursor-pointer text-[13px]"
                                    >✕</button>
                                )}
                            </div>
                        ) : (
                            <div
                                onDragOver={(e) => { e.preventDefault(); setDragOver(true); }}
                                onDragLeave={() => setDragOver(false)}
                                onDrop={handleDrop}
                                onClick={() => fileRef.current?.click()}
                                className={`border-2 border-dashed rounded-xl p-12 text-center cursor-pointer transition-colors
                                    ${dragOver ? 'border-[var(--accent)] bg-[var(--accent)]/5' : 'border-[var(--border-color)] hover:border-[var(--border-hover)]'}`}
                            >
                                <div className="w-16 h-16 rounded-full bg-[var(--bg-elevated)] flex items-center justify-center mx-auto mb-4">
                                    <Plus size={28} className="text-[var(--text-muted)]" />
                                </div>
                                <p className="text-[14px] text-[var(--text-primary)] mb-1">Drag photo here</p>
                                <p className="text-[12px] text-[var(--text-muted)]">or click to select</p>
                            </div>
                        )}
                    </div>

                    <input
                        ref={fileRef}
                        type="file"
                        accept="image/jpeg,image/png,image/webp"
                        className="hidden"
                        onChange={(e) => handleFile(e.target.files?.[0])}
                    />
                </motion.div>
            </motion.div>
        </AnimatePresence>
    );
}
