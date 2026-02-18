import { useState, useRef, useCallback } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { X, ImagePlus, Send, Loader, Type, Upload, Image as ImageIcon, CheckCircle } from 'lucide-react';
import { createPost, uploadImage } from '../api/posts';
import { useToast } from './Toast';

const MAX_CHARS = 500;
const MAX_FILE_SIZE = 5 * 1024 * 1024;
const ALLOWED_TYPES = ['image/jpeg', 'image/png', 'image/gif', 'image/webp'];

export default function CreatePostModal({ open, onClose, onPostCreated }) {
    const [content, setContent] = useState('');
    const [imageUrl, setImageUrl] = useState('');
    const [loading, setLoading] = useState(false);
    const [file, setFile] = useState(null);
    const [preview, setPreview] = useState('');
    const [uploading, setUploading] = useState(false);
    const [uploadProgress, setUploadProgress] = useState(0);
    const [dragOver, setDragOver] = useState(false);
    const fileRef = useRef(null);
    const toast = useToast();

    const charCount = content.length;
    const charPercent = (charCount / MAX_CHARS) * 100;
    const isOverLimit = charCount > MAX_CHARS;

    const validateFile = (f) => {
        if (!ALLOWED_TYPES.includes(f.type)) {
            toast.error('Invalid file type. Use JPEG, PNG, GIF, or WebP.');
            return false;
        }
        if (f.size > MAX_FILE_SIZE) {
            toast.error('File too large. Maximum 5MB allowed.');
            return false;
        }
        return true;
    };

    const handleFileSelect = (f) => {
        if (!validateFile(f)) return;
        setFile(f);
        setPreview(URL.createObjectURL(f));
        setImageUrl('');
    };

    const handleDrop = useCallback((e) => {
        e.preventDefault();
        setDragOver(false);
        const f = e.dataTransfer?.files?.[0];
        if (f) handleFileSelect(f);
    }, []);

    const handleDragOver = useCallback((e) => {
        e.preventDefault();
        setDragOver(true);
    }, []);

    const handleDragLeave = useCallback(() => {
        setDragOver(false);
    }, []);

    const removeFile = () => {
        setFile(null);
        setPreview('');
        setImageUrl('');
        setUploadProgress(0);
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        if (!content.trim() || isOverLimit) return;
        setLoading(true);

        try {
            let finalImageUrl = imageUrl;

            // Upload file if selected
            if (file && !imageUrl) {
                setUploading(true);
                setUploadProgress(0);
                const uploadRes = await uploadImage(file, (p) => setUploadProgress(p));
                finalImageUrl = uploadRes.data.imageUrl;
                setUploading(false);
            }

            await createPost({ content, imageUrl: finalImageUrl || null });
            setContent('');
            setImageUrl('');
            setFile(null);
            setPreview('');
            setUploadProgress(0);
            toast.success('Post published! 🚀');
            onPostCreated?.();
            onClose();
        } catch (err) {
            setUploading(false);
            toast.error(err.response?.data?.message || 'Failed to create post');
        } finally {
            setLoading(false);
        }
    };

    return (
        <AnimatePresence>
            {open && (
                <motion.div
                    initial={{ opacity: 0 }}
                    animate={{ opacity: 1 }}
                    exit={{ opacity: 0 }}
                    className="fixed inset-0 z-50 flex items-center justify-center px-4"
                >
                    <div className="absolute inset-0 bg-black/70 backdrop-blur-sm" onClick={onClose} />

                    <motion.div
                        initial={{ opacity: 0, scale: 0.92, y: 24 }}
                        animate={{ opacity: 1, scale: 1, y: 0 }}
                        exit={{ opacity: 0, scale: 0.92, y: 24 }}
                        transition={{ type: 'spring', damping: 28, stiffness: 300 }}
                        className="glass-strong rounded-2xl w-full max-w-lg relative z-10 max-h-[90vh] overflow-y-auto"
                    >
                        {/* Header */}
                        <div className="flex items-center justify-between p-5 border-b border-[var(--border-color)]">
                            <h2 className="text-base font-bold">Create Post</h2>
                            <button onClick={onClose} className="btn-icon w-8 h-8 border-0">
                                <X size={16} />
                            </button>
                        </div>

                        <form onSubmit={handleSubmit} className="p-5">
                            {/* Textarea */}
                            <div className="relative mb-4">
                                <textarea
                                    className="input-field min-h-[120px] resize-none text-[13px] leading-relaxed"
                                    placeholder="What's on your mind? Share your thoughts..."
                                    value={content}
                                    onChange={(e) => setContent(e.target.value)}
                                    autoFocus
                                    maxLength={MAX_CHARS + 50}
                                />
                                <div className="flex items-center justify-between mt-2">
                                    <div className="flex items-center gap-2">
                                        <Type size={12} className="text-[var(--text-muted)]" />
                                        <span className={`text-[11px] font-medium ${isOverLimit ? 'text-red-400' : charCount > MAX_CHARS * 0.8 ? 'text-[var(--warning)]' : 'text-[var(--text-muted)]'}`}>
                                            {charCount}/{MAX_CHARS}
                                        </span>
                                    </div>
                                    <svg width="20" height="20" className="-rotate-90">
                                        <circle cx="10" cy="10" r="8" fill="none" stroke="var(--border-color)" strokeWidth="2" />
                                        <circle
                                            cx="10" cy="10" r="8" fill="none"
                                            stroke={isOverLimit ? '#ff5a5a' : charCount > MAX_CHARS * 0.8 ? '#fbbf24' : 'var(--accent)'}
                                            strokeWidth="2"
                                            strokeDasharray={`${Math.min(charPercent, 100) * 0.5} 100`}
                                            strokeLinecap="round"
                                            className="transition-all duration-300"
                                        />
                                    </svg>
                                </div>
                            </div>

                            {/* Drag & Drop / File Picker */}
                            {!file && !preview && (
                                <div
                                    onDrop={handleDrop}
                                    onDragOver={handleDragOver}
                                    onDragLeave={handleDragLeave}
                                    onClick={() => fileRef.current?.click()}
                                    className={`mb-4 p-6 rounded-xl border-2 border-dashed transition-all cursor-pointer text-center ${dragOver
                                            ? 'border-[var(--accent)] bg-[var(--accent-light)]'
                                            : 'border-[var(--border-color)] hover:border-[var(--border-hover)] hover:bg-[var(--bg-elevated)]/30'
                                        }`}
                                >
                                    <input
                                        ref={fileRef}
                                        type="file"
                                        accept="image/jpeg,image/png,image/gif,image/webp"
                                        className="hidden"
                                        onChange={(e) => e.target.files?.[0] && handleFileSelect(e.target.files[0])}
                                    />
                                    <Upload size={24} className={`mx-auto mb-2 ${dragOver ? 'text-[var(--accent)]' : 'text-[var(--text-muted)]'}`} />
                                    <p className="text-[12px] text-[var(--text-secondary)] font-medium">
                                        {dragOver ? 'Drop image here' : 'Drag & drop an image, or click to browse'}
                                    </p>
                                    <p className="text-[10px] text-[var(--text-muted)] mt-1">JPEG, PNG, GIF, WebP · Max 5MB</p>
                                </div>
                            )}

                            {/* Image Preview */}
                            <AnimatePresence>
                                {preview && (
                                    <motion.div
                                        initial={{ opacity: 0, height: 0 }}
                                        animate={{ opacity: 1, height: 'auto' }}
                                        exit={{ opacity: 0, height: 0 }}
                                        className="mb-4 rounded-xl overflow-hidden border border-[var(--border-color)] relative group"
                                    >
                                        <img src={preview} alt="Preview" className="w-full h-44 object-cover" />
                                        <div className="absolute inset-0 bg-black/40 opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center">
                                            <button
                                                type="button"
                                                onClick={removeFile}
                                                className="p-2 rounded-full bg-black/60 hover:bg-black/80 transition-colors cursor-pointer"
                                            >
                                                <X size={16} className="text-white" />
                                            </button>
                                        </div>
                                        {/* Upload progress */}
                                        {uploading && (
                                            <div className="absolute bottom-0 left-0 right-0">
                                                <div className="bg-black/60 px-3 py-1.5 flex items-center gap-2">
                                                    <Loader size={12} className="text-[var(--accent)] animate-spin" />
                                                    <span className="text-[11px] text-white font-medium">Uploading {uploadProgress}%</span>
                                                </div>
                                                <div className="h-1 bg-[var(--bg-elevated)]">
                                                    <motion.div
                                                        className="h-full bg-gradient-to-r from-[var(--accent)] to-purple-400"
                                                        initial={{ width: 0 }}
                                                        animate={{ width: `${uploadProgress}%` }}
                                                        transition={{ ease: 'easeOut' }}
                                                    />
                                                </div>
                                            </div>
                                        )}
                                        {/* Upload complete indicator */}
                                        {imageUrl && !uploading && (
                                            <div className="absolute top-2 right-2 w-6 h-6 rounded-full bg-emerald-500 flex items-center justify-center">
                                                <CheckCircle size={14} className="text-white" />
                                            </div>
                                        )}
                                        {/* File info */}
                                        {file && (
                                            <div className="absolute bottom-0 left-0 right-0 bg-gradient-to-t from-black/60 to-transparent px-3 py-2">
                                                <div className="flex items-center gap-2">
                                                    <ImageIcon size={12} className="text-white/70" />
                                                    <span className="text-[10px] text-white/80 truncate">{file.name}</span>
                                                    <span className="text-[10px] text-white/60 ml-auto">{(file.size / 1024).toFixed(0)} KB</span>
                                                </div>
                                            </div>
                                        )}
                                    </motion.div>
                                )}
                            </AnimatePresence>

                            {/* OR URL input (fallback) */}
                            {!file && !preview && (
                                <div className="flex items-center gap-3 mb-4">
                                    <span className="text-[11px] text-[var(--text-muted)]">or paste URL:</span>
                                    <input
                                        type="text"
                                        className="input-field text-[12px] py-1.5 flex-1"
                                        placeholder="https://example.com/image.jpg"
                                        value={imageUrl}
                                        onChange={(e) => setImageUrl(e.target.value)}
                                    />
                                </div>
                            )}

                            {/* Submit */}
                            <motion.button
                                whileHover={{ scale: 1.01 }}
                                whileTap={{ scale: 0.99 }}
                                type="submit"
                                disabled={loading || uploading || !content.trim() || isOverLimit}
                                className="btn-primary w-full py-3"
                            >
                                {loading ? (
                                    <><Loader size={15} className="animate-spin" /> {uploading ? 'Uploading...' : 'Publishing...'}</>
                                ) : (
                                    <>Publish <Send size={15} /></>
                                )}
                            </motion.button>
                        </form>
                    </motion.div>
                </motion.div>
            )}
        </AnimatePresence>
    );
}
