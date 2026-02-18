import { useState, useEffect, useRef } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { Plus } from 'lucide-react';
import { getStories } from '../api/stories';
import StoryViewer from './StoryViewer';
import UploadStoryModal from './UploadStoryModal';

export default function StoriesBar() {
    const [storyUsers, setStoryUsers] = useState([]);
    const [viewerOpen, setViewerOpen] = useState(false);
    const [selectedUserIdx, setSelectedUserIdx] = useState(0);
    const [uploadOpen, setUploadOpen] = useState(false);
    const [loading, setLoading] = useState(true);
    const scrollRef = useRef(null);

    const fetchStories = () => {
        setLoading(true);
        getStories()
            .then((res) => setStoryUsers(res.data || []))
            .catch(() => { })
            .finally(() => setLoading(false));
    };

    useEffect(() => {
        fetchStories();
    }, []);

    const openStory = (idx) => {
        setSelectedUserIdx(idx);
        setViewerOpen(true);
    };

    return (
        <>
            <div className="border-b border-[var(--border-color)] py-3 mb-2">
                <div ref={scrollRef} className="flex items-center gap-4 overflow-x-auto px-3 scrollbar-hide">
                    {/* Add Story button */}
                    <motion.button
                        whileTap={{ scale: 0.9 }}
                        onClick={() => setUploadOpen(true)}
                        className="flex flex-col items-center gap-1.5 flex-shrink-0 cursor-pointer"
                    >
                        <div className="relative">
                            <div className="w-[62px] h-[62px] rounded-full bg-[var(--bg-elevated)] flex items-center justify-center border border-[var(--border-color)]">
                                <Plus size={24} className="text-[var(--text-muted)]" />
                            </div>
                            <div className="absolute -bottom-0.5 -right-0.5 w-5 h-5 rounded-full bg-[var(--accent)] flex items-center justify-center border-2 border-[var(--bg-primary)]">
                                <Plus size={10} className="text-white" strokeWidth={3} />
                            </div>
                        </div>
                        <span className="text-[10px] text-[var(--text-muted)] truncate w-[64px] text-center">Your story</span>
                    </motion.button>

                    {/* Loading skeletons */}
                    {loading && storyUsers.length === 0 && (
                        <>
                            {[1, 2, 3, 4, 5].map(i => (
                                <div key={i} className="flex flex-col items-center gap-1.5 flex-shrink-0">
                                    <div className="w-[62px] h-[62px] rounded-full skeleton" />
                                    <div className="w-10 h-2 skeleton rounded" />
                                </div>
                            ))}
                        </>
                    )}

                    {/* Story avatars */}
                    {storyUsers.map((su, idx) => {
                        const initial = su.name?.charAt(0)?.toUpperCase() || '?';
                        return (
                            <motion.button
                                key={su.userId}
                                whileHover={{ scale: 1.05 }}
                                whileTap={{ scale: 0.92 }}
                                onClick={() => openStory(idx)}
                                className="flex flex-col items-center gap-1.5 flex-shrink-0 cursor-pointer"
                            >
                                <div className={`p-[3px] rounded-full ${su.hasUnviewed
                                    ? 'bg-gradient-to-tr from-[#fcb045] via-[#fd1d1d] to-[#833ab4]'
                                    : 'bg-[var(--border-hover)]'}`}>
                                    <div className="w-[56px] h-[56px] rounded-full border-[3px] border-[var(--bg-primary)] overflow-hidden flex items-center justify-center"
                                        style={{ background: su.profilePicUrl ? 'transparent' : 'linear-gradient(135deg, #833ab4, #fd1d1d, #fcb045)' }}>
                                        {su.profilePicUrl ? (
                                            <img src={su.profilePicUrl} alt={su.name} className="w-full h-full object-cover" />
                                        ) : (
                                            <span className="text-white text-[16px] font-bold">{initial}</span>
                                        )}
                                    </div>
                                </div>
                                <span className="text-[10px] text-[var(--text-secondary)] truncate w-[64px] text-center">
                                    {su.name?.split(' ')[0]}
                                </span>
                            </motion.button>
                        );
                    })}
                </div>
            </div>

            {/* Story Viewer */}
            <AnimatePresence>
                {viewerOpen && storyUsers.length > 0 && (
                    <StoryViewer
                        storyUsers={storyUsers}
                        initialUserIndex={selectedUserIdx}
                        onClose={() => {
                            setViewerOpen(false);
                            fetchStories(); // Refresh to update viewed status
                        }}
                    />
                )}
            </AnimatePresence>

            {/* Upload Modal */}
            <UploadStoryModal
                open={uploadOpen}
                onClose={() => setUploadOpen(false)}
                onUploaded={fetchStories}
            />
        </>
    );
}
