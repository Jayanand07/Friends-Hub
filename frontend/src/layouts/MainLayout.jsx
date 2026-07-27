import { Outlet } from 'react-router-dom';
import { useState, useEffect } from 'react';
import Sidebar from '../components/Sidebar';
import Navbar from '../components/Navbar';
import ProfilePreview from '../components/ProfilePreview';
import BottomNav from '../components/BottomNav';
import CreatePostModal from '../components/CreatePostModal';
import { useAuth } from '../context/AuthContext';
import { connectChat, addNotificationHandler } from '../socket/chatSocket';
import { useToast } from '../components/Toast';

export default function MainLayout() {
    const { user } = useAuth();
    const toast = useToast();
    const [showCreate, setShowCreate] = useState(false);
    const [pendingNotifications, setPendingNotifications] = useState([]);
    const [isOffline, setIsOffline] = useState(!navigator.onLine);
    const currentUserId = user?.id;

    // Offline/online detection
    useEffect(() => {
        const goOffline = () => {
            setIsOffline(true);
            toast.error('You are offline. Some features may be unavailable.');
        };
        const goOnline = () => {
            setIsOffline(false);
            toast.success('Back online!');
        };

        window.addEventListener('app:offline', goOffline);
        window.addEventListener('app:online', goOnline);
        window.addEventListener('offline', goOffline);
        window.addEventListener('online', goOnline);

        return () => {
            window.removeEventListener('app:offline', goOffline);
            window.removeEventListener('app:online', goOnline);
            window.removeEventListener('offline', goOffline);
            window.removeEventListener('online', goOnline);
        };
    }, [toast]);

    // Subscribe to notification queue — shares the existing STOMP connection from chatSocket
    useEffect(() => {
        if (!currentUserId) return;

        // Ensure the connection is active (idempotent — won't create duplicate)
        connectChat(currentUserId, {});

        // Register notification handler on the shared connection
        const unsubscribe = addNotificationHandler(currentUserId, (body) => {
            setPendingNotifications(prev => [...prev, body]);
        });

        return () => {
            unsubscribe();
        };
    }, [currentUserId]);

    return (
        <div className="flex min-h-screen relative">
            {/* Offline banner */}
            {isOffline && (
                <div className="fixed top-0 left-0 right-0 z-[200] bg-red-600 text-white text-center text-[12px] font-semibold py-1.5">
                    You are offline. Some features may be unavailable.
                </div>
            )}
            {/* Floating orbs */}
            <div className="orb orb-1" />
            <div className="orb orb-2" />
            <div className="orb orb-3" />

            <Sidebar onCreatePost={() => setShowCreate(true)} isCreateOpen={showCreate} />
            <div className="flex-1 flex flex-col relative z-10">
                <Navbar pendingNotifications={pendingNotifications} />
                <div className="flex flex-1">
                    <main className="flex-1 max-w-2xl mx-auto w-full px-4 py-6 pb-24 lg:pb-6">
                        <Outlet context={{ setShowCreate }} />
                    </main>
                    <ProfilePreview />
                </div>
            </div>

            {/* Mobile bottom nav */}
            <BottomNav onCreatePost={() => setShowCreate(true)} />

            {/* Shared create modal for mobile FAB */}
            <CreatePostModal
                open={showCreate}
                onClose={() => setShowCreate(false)}
                onPostCreated={() => {
                    setShowCreate(false);
                    window.dispatchEvent(new Event('refreshFeed'));
                }}
            />
        </div>
    );
}
