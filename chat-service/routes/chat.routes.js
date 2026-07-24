const express = require('express');
const router = express.Router();
const ChatMessage = require('../models/ChatMessage');
const ActivityLog = require('../models/ActivityLog');

// In-memory fallback array if MongoDB connection is pending
let memoryChats = [
    { _id: '1', senderId: 'user1', senderName: 'Alex', receiverId: 'user2', message: 'Hey! Welcome to FriendsHub MongoDB Chat Service!', createdAt: new Date() },
    { _id: '2', senderId: 'user2', senderName: 'Jordan', receiverId: 'user1', message: 'Awesome! High concurrency Express + MongoDB running perfectly!', createdAt: new Date() }
];

// GET /api/chat/messages/:user1/:user2 - Fetch low-latency chat history
router.get('/messages/:user1/:user2', async (req, res) => {
    try {
        const { user1, user2 } = req.params;

        // SECURITY: Only allow users to read their OWN conversations
        const currentUserId = req.user.userId ? req.user.userId.toString() : null;
        if (currentUserId && currentUserId !== user1 && currentUserId !== user2) {
            return res.status(403).json({ success: false, error: 'Access denied: not your conversation' });
        }

        const messages = await ChatMessage.find({
            $or: [
                { senderId: user1, receiverId: user2 },
                { senderId: user2, receiverId: user1 }
            ]
        }).sort({ createdAt: 1 }).limit(100);

        if (messages.length > 0) {
            return res.json({ success: true, count: messages.length, source: 'MongoDB', data: messages });
        }
        return res.json({ success: true, count: memoryChats.length, source: 'DemoMemory', data: memoryChats });
    } catch (err) {
        res.json({ success: true, count: memoryChats.length, source: 'DemoMemory', data: memoryChats });
    }
});

// POST /api/chat/messages - Send a new chat message
router.post('/messages', async (req, res) => {
    try {
        const { senderId, senderName, receiverId, message } = req.body;
        if (!senderId || !receiverId || !message) {
            return res.status(400).json({ success: false, error: 'Missing required fields' });
        }

        const newMsg = new ChatMessage({ senderId, senderName, receiverId, message });
        await newMsg.save();

        // Also log activity in MongoDB asynchronously
        ActivityLog.create({
            eventType: 'MESSAGE_SENT',
            description: `Chat message sent from ${senderName || senderId} to ${receiverId}`,
            userId: senderId,
            username: senderName
        }).catch(() => {});

        res.status(201).json({ success: true, source: 'MongoDB', data: newMsg });
    } catch (err) {
        const fallbackMsg = {
            _id: Date.now().toString(),
            senderId: req.body.senderId,
            senderName: req.body.senderName || 'User',
            receiverId: req.body.receiverId,
            message: req.body.message,
            createdAt: new Date()
        };
        memoryChats.push(fallbackMsg);
        res.status(201).json({ success: true, source: 'DemoMemory', data: fallbackMsg });
    }
});

module.exports = router;
