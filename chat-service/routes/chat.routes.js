const express = require('express');
const router = express.Router();
const ChatMessage = require('../models/ChatMessage');
const ActivityLog = require('../models/ActivityLog');

// In-memory fallback store when MongoDB is unavailable.
// FIX: this used to be pre-seeded with fake demo conversations ("Alex"/"Jordan"),
// which were returned to real users as if they were their chat history.
// It is now empty — real data only.
let memoryChats = [];

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
        return res.json({ success: true, count: memoryChats.length, source: 'MemoryFallback', data: memoryChats });
    } catch (err) {
        res.json({ success: true, count: memoryChats.length, source: 'MemoryFallback', data: memoryChats });
    }
});

// POST /api/chat/messages - Send a new chat message
router.post('/messages', async (req, res) => {
    try {
        // SECURITY: Override senderId/senderName from the authenticated JWT — never trust the client
        const senderId = req.user.userId ? req.user.userId.toString() : null;
        const senderName = req.user.email ? req.user.email.split('@')[0] : 'User';
        const { receiverId, message } = req.body;
        if (!senderId || !receiverId || !message) {
            return res.status(400).json({ success: false, error: 'Missing required fields' });
        }
        // Validate message shape/size before persisting (16KB JSON cap is set in server.js)
        if (typeof message !== 'string' || message.trim().length === 0 || message.length > 5000) {
            return res.status(400).json({ success: false, error: 'Message must be 1-5000 characters' });
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
        // SECURITY: Fallback also uses authenticated user, not request body
        const fallbackSenderId = req.user.userId ? req.user.userId.toString() : null;
        const fallbackSenderName = req.user.email ? req.user.email.split('@')[0] : 'User';
        const fallbackMsg = {
            _id: Date.now().toString(),
            senderId: fallbackSenderId,
            senderName: fallbackSenderName,
            receiverId: req.body.receiverId,
            message: req.body.message,
            createdAt: new Date()
        };
        memoryChats.push(fallbackMsg);
        res.status(201).json({ success: true, source: 'DemoMemory', data: fallbackMsg });
    }
});

module.exports = router;
