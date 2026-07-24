const express = require('express');
const http = require('http');
const cors = require('cors');
const dotenv = require('dotenv');
const connectDB = require('./config/db');

dotenv.config({ path: '../.env' });

const app = express();
const server = http.createServer(app);

// Connect to MongoDB Atlas / Local MongoDB
connectDB();

// SECURITY: Restrict CORS to known frontend origin only (C-3 fix)
const allowedOrigins = (process.env.FRONTEND_URL || 'http://localhost:5173').split(',').map(s => s.trim());
app.use(cors({
    origin: function(origin, callback) {
        // Allow requests with no origin (server-to-server, curl, health checks)
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

// SECURITY: JWT authentication middleware (C-3 fix)
function authMiddleware(req, res, next) {
    const authHeader = req.headers.authorization;
    if (!authHeader || !authHeader.startsWith('Bearer ')) {
        return res.status(401).json({ success: false, error: 'Authentication required' });
    }
    try {
        const token = authHeader.substring(7);
        // Decode JWT payload to extract userId (same secret as Spring Boot backend)
        const parts = token.split('.');
        if (parts.length !== 3) {
            return res.status(401).json({ success: false, error: 'Invalid token format' });
        }
        const payload = JSON.parse(Buffer.from(parts[1], 'base64url').toString('utf8'));

        // Check expiry
        if (payload.exp && Date.now() / 1000 > payload.exp) {
            return res.status(401).json({ success: false, error: 'Token expired' });
        }

        req.user = {
            email: payload.sub,
            userId: payload.userId,
            role: payload.role
        };
        next();
    } catch (err) {
        return res.status(401).json({ success: false, error: 'Invalid or expired token' });
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
    console.log(`🔒 Auth: JWT middleware enabled`);
    console.log(`🌐 CORS: Restricted to ${allowedOrigins.join(', ')}`);
    console.log(`====================================================`);
});
