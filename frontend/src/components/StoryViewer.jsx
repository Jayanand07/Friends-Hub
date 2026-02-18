import { useState, useEffect, useCallback, useRef } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { ChevronLeft, ChevronRight, X, Eye, Pause, Play } from 'lucide-react';
import { viewStory } from '../api/stories';

const STORY_DURATION = 5000; // 5 seconds per story

export default function StoryViewer({ storyUsers, initialUserIndex, onClose }) {
    const [userIdx, setUserIdx] = useState(initialUserIndex || 0);
    const [storyIdx, setStoryIdx] = useState(0);
    const [progress, setProgress] = useState(0);
    const [paused, setPaused] = useState(false);
    const [direction, setDirection] = useState(0);
    const timerRef = useRef(null);
    const startTimeRef = useRef(null);
    const elapsedRef = useRef(0);

    const currentUserStories = storyUsers[userIdx];
    const currentStory = currentUserStories?.stories?.[storyIdx];
    const initial = currentUserStories?.name?.charAt(0)?.toUpperCase() || '?';

    // Mark story as viewed
    useEffect(() => {
        if (currentStory?.storyId) {
            viewStory(currentStory.storyId).catch(() => { });
        }
    }, [currentStory?.storyId]);

    // Auto-advance timer
    const startTimer = useCallback(() => {
        startTimeRef.current = Date.now();
        timerRef.current = setInterval(() => {
            const elapsed = elapsedRef.current + (Date.now() - startTimeRef.current);
            const pct = Math.min((elapsed / STORY_DURATION) * 100, 100);
            setProgress(pct);
            if (pct >= 100) {
                clearInterval(timerRef.current);
                goNext();
            }
        }, 30);
    }, [userIdx, storyIdx]);

    const pauseTimer = useCallback(() => {
        if (timerRef.current) {
            clearInterval(timerRef.current);
            elapsedRef.current += Date.now() - startTimeRef.current;
        }
    }, []);

    useEffect(() => {
        elapsedRef.current = 0;
        setProgress(0);
        if (!paused) startTimer();
        return () => clearInterval(timerRef.current);
    }, [userIdx, storyIdx, paused]);

    const goNext = () => {
        const stories = storyUsers[userIdx]?.stories;
        if (storyIdx < stories.length - 1) {
            setDirection(1);
            setStoryIdx(storyIdx + 1);
        } else if (userIdx < storyUsers.length - 1) {
            setDirection(1);
            setUserIdx(userIdx + 1);
            setStoryIdx(0);
        } else {
            onClose();
        }
    };

    const goPrev = () => {
        if (storyIdx > 0) {
            setDirection(-1);
            setStoryIdx(storyIdx - 1);
        } else if (userIdx > 0) {
            setDirection(-1);
            const prevStories = storyUsers[userIdx - 1]?.stories;
            setUserIdx(userIdx - 1);
            setStoryIdx(prevStories ? prevStories.length - 1 : 0);
        }
    };

    const togglePause = () => {
        if (paused) {
            setPaused(false);
            startTimer();
        } else {
            setPaused(true);
            pauseTimer();
        }
    };

    // Keyboard navigation
    useEffect(() => {
        const handleKey = (e) => {
            if (e.key === 'ArrowRight' || e.key === ' ') { e.preventDefault(); goNext(); }
            if (e.key === 'ArrowLeft') goPrev();
            if (e.key === 'Escape') onClose();
            if (e.key === 'p') togglePause();
        };
        window.addEventListener('keydown', handleKey);
        return () => window.removeEventListener('keydown', handleKey);
    }, [userIdx, storyIdx, paused]);

    if (!currentStory) return null;

    const stories = currentUserStories.stories;

    // Time ago for story
    const timeAgo = (dateStr) => {
        if (!dateStr) return '';
        const date = new Date(dateStr);
        if (isNaN(date.getTime())) return '';
        const diff = Date.now() - date.getTime();
        const mins = Math.floor(diff / 60000);
        if (mins < 1) return 'Just now';
        if (mins < 60) return `${mins}m`;
        return `${Math.floor(mins / 60)}h`;
    };

    return (
        <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="fixed inset-0 z-[70] bg-black flex items-center justify-center"
        >
            {/* Close */}
            <button onClick={onClose} className="absolute top-4 right-4 z-10 w-9 h-9 flex items-center justify-center text-white/80 hover:text-white cursor-pointer">
                <X size={24} />
            </button>

            {/* Prev arrow */}
            {(userIdx > 0 || storyIdx > 0) && (
                <button onClick={goPrev} className="absolute left-2 sm:left-6 z-10 w-9 h-9 rounded-full bg-white/10 flex items-center justify-center text-white hover:bg-white/20 cursor-pointer transition-colors">
                    <ChevronLeft size={20} />
                </button>
            )}

            {/* Next arrow */}
            {(userIdx < storyUsers.length - 1 || storyIdx < stories.length - 1) && (
                <button onClick={goNext} className="absolute right-2 sm:right-6 z-10 w-9 h-9 rounded-full bg-white/10 flex items-center justify-center text-white hover:bg-white/20 cursor-pointer transition-colors">
                    <ChevronRight size={20} />
                </button>
            )}

            {/* Story content */}
            <div className="relative w-full max-w-[420px] h-full max-h-[750px] mx-auto">
                {/* Progress bars */}
                <div className="absolute top-0 left-0 right-0 z-10 flex gap-1 px-3 pt-3">
                    {stories.map((_, i) => (
                        <div key={i} className="flex-1 h-[2px] bg-white/20 rounded-full overflow-hidden">
                            <div
                                className="h-full bg-white rounded-full transition-all duration-75"
                                style={{
                                    width: i < storyIdx ? '100%' : i === storyIdx ? `${progress}%` : '0%'
                                }}
                            />
                        </div>
                    ))}
                </div>

                {/* Header */}
                <div className="absolute top-5 left-0 right-0 z-10 flex items-center gap-3 px-4 pt-2">
                    <div className="w-8 h-8 rounded-full overflow-hidden flex items-center justify-center text-[10px] font-bold"
                        style={{ background: currentUserStories.profilePicUrl ? 'transparent' : 'linear-gradient(135deg, #833ab4, #fd1d1d, #fcb045)' }}>
                        {currentUserStories.profilePicUrl ? (
                            <img src={currentUserStories.profilePicUrl} alt="" className="w-full h-full object-cover" />
                        ) : (
                            <span className="text-white">{initial}</span>
                        )}
                    </div>
                    <div className="flex-1 min-w-0">
                        <p className="text-[13px] font-semibold text-white truncate">{currentUserStories.name}</p>
                    </div>
                    <span className="text-[12px] text-white/60">{timeAgo(currentStory.createdAt)}</span>
                    <button onClick={togglePause} className="text-white/80 hover:text-white cursor-pointer ml-1">
                        {paused ? <Play size={16} /> : <Pause size={16} />}
                    </button>
                </div>

                {/* Image */}
                <AnimatePresence mode="wait" initial={false}>
                    <motion.img
                        key={`${userIdx}-${storyIdx}`}
                        src={currentStory.imageUrl}
                        alt="Story"
                        initial={{ opacity: 0, x: direction * 40 }}
                        animate={{ opacity: 1, x: 0 }}
                        exit={{ opacity: 0, x: direction * -40 }}
                        transition={{ duration: 0.2 }}
                        className="w-full h-full object-contain rounded-lg"
                        onClick={(e) => {
                            const rect = e.currentTarget.getBoundingClientRect();
                            const x = e.clientX - rect.left;
                            if (x < rect.width / 3) goPrev();
                            else goNext();
                        }}
                        onMouseDown={() => { setPaused(true); pauseTimer(); }}
                        onMouseUp={() => { setPaused(false); }}
                    />
                </AnimatePresence>

                {/* Footer — viewer count (only for own stories) */}
                {currentStory.viewerCount > 0 && (
                    <div className="absolute bottom-4 left-0 right-0 flex justify-center">
                        <div className="flex items-center gap-1.5 px-3 py-1.5 bg-black/50 rounded-full">
                            <Eye size={14} className="text-white/70" />
                            <span className="text-[12px] text-white/70">{currentStory.viewerCount}</span>
                        </div>
                    </div>
                )}

                {/* Tap zones */}
                <div className="absolute inset-0 flex">
                    <div className="w-1/3 h-full cursor-pointer" onClick={goPrev} />
                    <div className="w-1/3 h-full" onClick={togglePause} />
                    <div className="w-1/3 h-full cursor-pointer" onClick={goNext} />
                </div>
            </div>
        </motion.div>
    );
}
