import { useState, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { X, Search, Check, Loader } from 'lucide-react';
import { searchChatUsers } from '../../api/chat';
import { getSuggestions } from '../../api/users';
import { createGroup } from '../../api/groupChat';
import { uploadImage } from '../../api/posts';
import { useToast } from '../Toast';
import { useAuth } from '../../context/AuthContext';
import { generateGroupKey, getPublicKeyForUser, encryptMessage } from '../../crypto/e2ee';

export default function CreateGroupModal({ onClose, onCreated }) {
    const { user: currentUser } = useAuth();
    const [name, setName] = useState('');
    const [imageFile, setImageFile] = useState(null);
    const [imagePreview, setImagePreview] = useState('');
    const [query, setQuery] = useState('');
    const [allUsers, setAllUsers] = useState([]);
    const [results, setResults] = useState([]);
    const [selectedUsers, setSelectedUsers] = useState([]);
    const [creating, setCreating] = useState(false);
    const [loadingUsers, setLoadingUsers] = useState(true);
    const toast = useToast();

    // Load initial user list on mount
    useEffect(() => {
        getSuggestions()
            .then(res => {
                const users = Array.isArray(res.data) ? res.data : [];
                setAllUsers(users);
                setResults(users);
            })
            .catch(() => toast.error('Failed to load user suggestions'))
            .finally(() => setLoadingUsers(false));
    }, []);

    // Filter locally as user types + debounced API search for broader results
    useEffect(() => {
        const trimmed = query.trim();

        // Immediate local filter for instant feedback
        if (trimmed) {
            const lower = trimmed.toLowerCase();
            const localFiltered = allUsers.filter(u =>
                u.name?.toLowerCase().includes(lower) ||
                u.username?.toLowerCase().includes(lower)
            );
            setResults(localFiltered);
        } else {
            setResults(allUsers);
        }

        // Debounced API search to broaden results beyond the initial suggestion list
        const timer = setTimeout(() => {
            if (trimmed) {
                searchChatUsers(trimmed)
                    .then(res => {
                        const apiResults = Array.isArray(res.data) ? res.data : [];
                        // Merge API results with local filter, deduplicated by id
                        setResults(prev => {
                            const ids = new Set(prev.map(u => u.id));
                            const merged = [...prev, ...apiResults.filter(u => !ids.has(u.id))];
                            return merged;
                        });
                    })
                    .catch(() => { });
            }
        }, 300);
        return () => clearTimeout(timer);
    }, [query, allUsers]);

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

            const rawMemberIds = selectedUsers.map(u => u.id || u.userId);
            const allMemberIds = Array.from(new Set([...rawMemberIds, currentUser.id]));

            // Generate group key
            const groupKeyB64 = await generateGroupKey();
            const groupKeysMap = {};

            // Encrypt group key for each member
            for (const uid of allMemberIds) {
                try {
                    const memberPubKey = await getPublicKeyForUser(uid);
                    if (memberPubKey) {
                        const encryptedKeyObj = await encryptMessage(groupKeyB64, memberPubKey);
                        groupKeysMap[uid] = encryptedKeyObj;
                    }
                } catch (e) {
                    console.error(`Failed to encrypt group key for user ${uid}:`, e);
                }
            }

            const groupKeysJSON = JSON.stringify(groupKeysMap);
            await createGroup(name, imageUrl, rawMemberIds, groupKeysJSON);
            toast.success("Group created with E2EE!");
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
                            {loadingUsers ? (
                                <div className="flex items-center justify-center py-6">
                                    <Loader size={18} className="animate-spin text-[var(--text-muted)]" />
                                </div>
                            ) : results.length === 0 ? (
                                <p className="text-center text-xs text-[var(--text-muted)] py-6">
                                    {query.trim() ? 'No users found' : 'No suggestions available'}
                                </p>
                            ) : results.map(user => {
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
