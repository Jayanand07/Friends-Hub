
import { Client } from '@stomp/stompjs';
import WebSocket from 'ws';
import axios from 'axios';

Object.assign(global, { WebSocket });

const API_URL = 'http://localhost:8080/api';
const EMAIL = 'testrealtime@example.com';
const PASSWORD = 'password123';

async function runTest() {
    try {
        console.log("1. Registering/Logging in...");
        try {
            await axios.post(`${API_URL}/auth/register`, {
                firstName: 'Realtime', lastName: 'Tester', email: EMAIL, password: PASSWORD
            });
        } catch (e) { /* User might exist */ }

        const loginRes = await axios.post(`${API_URL}/auth/login`, { email: EMAIL, password: PASSWORD });
        const token = loginRes.data.token;
        console.log("Logged in. Token acquired.");

        console.log("2. Fetching Profile...");
        const profileRes = await axios.get(`${API_URL}/users/profile`, { headers: { Authorization: `Bearer ${token}` } });
        const userId = profileRes.data.id || profileRes.data.userId;
        console.log(`User ID: ${userId}`);

        console.log("3. Connecting WebSocket...");
        // Spring Boot SockJS fallback usually at /ws/websocket
        const client = new Client({
            brokerURL: 'ws://localhost:8080/ws/websocket',
            connectHeaders: {},
            debug: (str) => console.log(str),
            reconnectDelay: 0,
        });

        client.onConnect = (frame) => {
            console.log("Connected to Broker.");

            // Subscribe to personal messages
            const dest = `/queue/messages-${userId}`;
            console.log(`Subscribing to ${dest}`);

            client.subscribe(dest, (message) => {
                console.log("RECEIVED MESSAGE:", message.body);
                client.deactivate();
                console.log("Test Passed!");
                process.exit(0);
            });

            // Send a message to self via REST API
            console.log("4. Sending Message via REST...");
            // Need to match ChatSendRequest DTO
            axios.post(`${API_URL}/chat/send`, {
                receiverId: userId,
                content: "Hello from Realtime Test!",
                imageUrl: null
            }, { headers: { Authorization: `Bearer ${token}` } })
                .then(() => console.log("Message Sent via REST"))
                .catch(err => {
                    console.error("Failed to send message:", err.response?.data || err.message);
                    client.deactivate();
                    process.exit(1);
                });
        };

        client.onStompError = (frame) => {
            console.error('Broker reported error: ' + frame.headers['message']);
            console.error('Additional details: ' + frame.body);
            process.exit(1);
        };

        client.onWebSocketError = (event) => {
            console.error('WebSocket Error', event);
            process.exit(1);
        }

        client.activate();

    } catch (err) {
        console.error("Test Failed:", err.response?.data || err.message);
        process.exit(1);
    }
}

runTest();
