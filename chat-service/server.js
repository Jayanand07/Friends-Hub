const express = require('express');
const http = require('http');
const cors = require('cors');
const jwt = require('jsonwebtoken');
const dotenv = require('dotenv');
const connectDB = require('./config/db');

dotenv.config({ path: '../.env' });

const app = express();
const server = http.createServer(app);

// Connect to MongoDB Atlas / Local MongoDB
connectDB();

// SECURITY: Restrict CORS to known frontend origin only
const allowedOrigins = (process.env.FRONTEND_URL || 'http://localhost:5173').split(',').map(s => s.trim());
app.use(cors({
    origin: function(origin, callback) {
        if (!origin) return callback(null, true);
        if (allowedOrigins.includes(origin)) {
            callback(null, true);
        } else {
            callback(new Error('CORS policy: Origin not allowed'));
        }
    },
    credentials: true
}));
app.use(express.json());

// JWT Secret Key (same as Spring Boot backend)
const JWT_SECRET = process.env.JWT_SECRET || 'secret_key_change_me_in_production_environment_12345';

// SECURITY: Full cryptographic JWT signature verification middleware
function authMiddleware(req, res, next) {
    const authHeader = req.headers.authorization;
    if (!authHeader || !authHeader.startsWith('Bearer ')) {
        return res.status(401).json({ success: false, error: 'Authentication required' });
    }
    try {
        const token = authHeader.substring(7);
        // Cryptographically verify signature and check expiry using JWT_SECRET
        const decoded = jwt.verify(token, JWT_SECRET);

        req.user = {
            email: decoded.sub,
            userId: decoded.userId,
            role: decoded.role
        };
        next();
    } catch (err) {
        return res.status(401).json({ success: false, error: 'Invalid or expired token signature' });
    }
}

// Apply auth middleware to protected routes
app.use('/api/chat', authMiddleware, require('./routes/chat.routes'));
app.use('/api/analytics', authMiddleware, require('./routes/analytics.routes'));

// Health check endpoint (public, no auth required)
app.get('/health', (req, res) => {
    res.json({
        status: 'UP',
        service: 'FriendsHub Node.js + MongoDB Microservice',
        database: 'MongoDB Atlas / Local',
        port: process.env.CHAT_SERVICE_PORT || 5000
    });
});

const PORT = process.env.CHAT_SERVICE_PORT || 5000;
server.listen(PORT, () => {
    console.log(`====================================================`);
    console.log(`⚡ FriendsHub Node.js + Express + MongoDB Service`);
    console.log(`🚀 Running on: http://localhost:${PORT}`);
    console.log(`🔒 Auth: JWT signature verification enabled`);
    console.log(`🌐 CORS: Restricted to ${allowedOrigins.join(', ')}`);
    console.log(`====================================================`);
});
