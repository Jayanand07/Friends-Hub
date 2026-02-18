import { Outlet } from 'react-router-dom';
import { useState, useEffect, useRef } from 'react';
import Sidebar from '../components/Sidebar';
import Navbar from '../components/Navbar';
import ProfilePreview from '../components/ProfilePreview';
import BottomNav from '../components/BottomNav';
import CreatePostModal from '../components/CreatePostModal';
import SockJS from 'sockjs-client/dist/sockjs';
import { Client } from '@stomp/stompjs';
import { useAuth } from '../context/AuthContext';

export default function MainLayout() {
    const { user } = useAuth();
    const [showCreate, setShowCreate] = useState(false);
    const [newNotification, setNewNotification] = useState(null);
    const currentUserId = user?.id;
    const clientRef = useRef(null);

    // Subscribe to notification queue globally
    useEffect(() => {
        if (!currentUserId) return;

        const token = localStorage.getItem('token');
        const client = new Client({
            webSocketFactory: () => new SockJS('http://localhost:8080/ws'),
            connectHeaders: {
                Authorization: `Bearer ${token}`
            },
            reconnectDelay: 5000,
            debug: () => { },
        });

        client.onConnect = () => {
            client.subscribe(`/queue/notifications-${currentUserId}`, (msg) => {
                const body = JSON.parse(msg.body);
                setNewNotification(body);
            });
        };

        client.activate();
        clientRef.current = client;

        return () => {
            client.deactivate();
            clientRef.current = null;
        };
    }, [currentUserId]);

    return (
        <div className="flex min-h-screen relative">
            {/* Floating orbs */}
            <div className="orb orb-1" />
            <div className="orb orb-2" />
            <div className="orb orb-3" />

            <Sidebar onCreatePost={() => setShowCreate(true)} />
            <div className="flex-1 flex flex-col relative z-10">
                <Navbar newNotification={newNotification} />
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
