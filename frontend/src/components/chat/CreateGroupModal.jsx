import { useState, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { X, Search, Check, Loader } from 'lucide-react';
import { searchChatUsers } from '../../api/chat';
import { createGroup } from '../../api/groupChat';
import { uploadImage } from '../../api/posts';
import { useToast } from '../Toast'; // Fixed import path

export default function CreateGroupModal({ onClose, onCreated }) {
    const [name, setName] = useState('');
    const [imageFile, setImageFile] = useState(null);
    const [imagePreview, setImagePreview] = useState('');
    const [query, setQuery] = useState('');
    const [results, setResults] = useState([]);
    const [selectedUsers, setSelectedUsers] = useState([]);
    const [creating, setCreating] = useState(false);
    const toast = useToast();

    // Search users
    useEffect(() => {
        const timer = setTimeout(() => {
            if (query.trim()) {
                searchChatUsers(query)
                    .then(res => setResults(Array.isArray(res.data) ? res.data : []))
                    .catch(() => { });
            } else {
                setResults([]);
            }
        }, 300);
        return () => clearTimeout(timer);
    }, [query]);

    const handleCreate = async () => {
        if (!name.trim()) return toast.error("Group name is required");
        if (selectedUsers.length === 0) return toast.error("Add at least one member");

        setCreating(true);
        try {
            let imageUrl = null;
            if (imageFile) {
                const uploadRes = await uploadImage(imageFile);
                imageUrl = uploadRes.data.imageUrl;
            }

            const memberIds = selectedUsers.map(u => u.id || u.userId);
            await createGroup(name, imageUrl, memberIds);
            toast.success("Group created!");
            onCreated();
        } catch (err) {
            console.error(err);
            toast.error("Failed to create group");
        } finally {
            setCreating(false);
        }
    };

    const toggleUser = (user) => {
        if (selectedUsers.find(u => u.id === user.id)) {
            setSelectedUsers(prev => prev.filter(u => u.id !== user.id));
        } else {
            setSelectedUsers(prev => [...prev, user]);
        }
    };

    return (
        <div className="fixed inset-0 bg-black/80 backdrop-blur-sm z-[100] flex items-center justify-center p-4">
            <motion.div
                initial={{ opacity: 0, scale: 0.95 }}
                animate={{ opacity: 1, scale: 1 }}
                className="bg-[var(--bg-card)] border border-[var(--border-color)] w-full max-w-md rounded-2xl overflow-hidden shadow-2xl"
            >
                <div className="p-4 border-b border-[var(--border-color)] flex justify-between items-center">
                    <h2 className="text-lg font-bold">New Group</h2>
                    <button onClick={onClose}><X size={20} /></button>
                </div>

                <div className="p-4 space-y-4">
                    {/* Image Upload */}
                    <div className="flex justify-center">
                        <div className="relative group cursor-pointer w-20 h-20 rounded-full bg-[var(--bg-elevated)] flex items-center justify-center overflow-hidden border-2 border-dashed border-[var(--border-color)]">
                            {imagePreview ? (
                                <img src={imagePreview} className="w-full h-full object-cover" />
                            ) : (
                                <span className="text-xs text-[var(--text-muted)]">Add Photo</span>
                            )}
                            <input
                                type="file"
                                onChange={e => {
                                    const file = e.target.files[0];
                                    if (file) {
                                        setImageFile(file);
                                        const reader = new FileReader();
                                        reader.onloadend = () => setImagePreview(reader.result);
                                        reader.readAsDataURL(file);
                                    }
                                }}
                                className="absolute inset-0 opacity-0 cursor-pointer"
                            />
                        </div>
                    </div>

                    {/* Name Input */}
                    <input
                        type="text"
                        value={name}
                        onChange={e => setName(e.target.value)}
                        placeholder="Group Name"
                        className="w-full bg-[var(--bg-elevated)] border border-[var(--border-color)] rounded-lg px-4 py-2.5 outline-none focus:border-[var(--accent)] transition-colors"
                    />

                    {/* Member Search */}
                    <div>
                        <div className="relative">
                            <Search className="absolute left-3 top-3 text-[var(--text-muted)]" size={16} />
                            <input
                                type="text"
                                value={query}
                                onChange={e => setQuery(e.target.value)}
                                placeholder="Search people..."
                                className="w-full bg-[var(--bg-elevated)] border border-[var(--border-color)] rounded-lg pl-10 pr-4 py-2.5 outline-none focus:border-[var(--accent)] transition-colors"
                            />
                        </div>

                        {/* Selected chips */}
                        {selectedUsers.length > 0 && (
                            <div className="flex flex-wrap gap-2 mt-3">
                                {selectedUsers.map(u => (
                                    <span key={u.id} className="bg-[var(--accent-light)] text-[var(--accent)] text-xs font-medium px-2 py-1 rounded-full flex items-center gap-1">
                                        {u.name}
                                        <X size={12} className="cursor-pointer" onClick={() => toggleUser(u)} />
                                    </span>
                                ))}
                            </div>
                        )}

                        {/* Search Results */}
                        <div className="mt-3 max-h-48 overflow-y-auto custom-scrollbar space-y-2">
                            {results.map(user => {
                                const isSelected = selectedUsers.find(u => u.id === user.id);
                                return (
                                    <div
                                        key={user.id}
                                        onClick={() => toggleUser(user)}
                                        className="flex items-center justify-between p-2 hover:bg-[var(--bg-elevated)] rounded-lg cursor-pointer"
                                    >
                                        <div className="flex items-center gap-3">
                                            <div className="w-8 h-8 rounded-full bg-[var(--bg-elevated)] flex items-center justify-center text-[10px] font-bold">
                                                {user.name?.[0]}
                                            </div>
                                            <span className="text-sm">{user.name}</span>
                                        </div>
                                        {isSelected && <Check size={16} className="text-[var(--accent)]" />}
                                    </div>
                                );
                            })}
                        </div>
                    </div>
                </div>

                <div className="p-4 border-t border-[var(--border-color)] flex justify-end">
                    <button
                        onClick={handleCreate}
                        disabled={creating}
                        className="bg-[var(--accent)] text-white px-6 py-2 rounded-lg font-medium hover:bg-[var(--accent-hover)] disabled:opacity-50 flex items-center gap-2"
                    >
                        {creating && <Loader size={16} className="animate-spin" />}
                        Create Group
                    </button>
                </div>
            </motion.div>
        </div>
    );
}
