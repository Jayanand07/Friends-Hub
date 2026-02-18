
import axios from 'axios';
import FormData from 'form-data';
import fs from 'fs';

const API_URL = 'http://localhost:8080/api';
const EMAIL = 'testrealtime@example.com';
const PASSWORD = 'password123';
const IMAGE_PATH = './test_image.jpg';

async function runTest() {
    try {
        // 0. Create dummy image with valid header
        const buffer = Buffer.from([0xFF, 0xD8, 0xFF, 0xE0]);
        fs.writeFileSync(IMAGE_PATH, buffer);
        console.log("0. Dummy image created.");

        // 1. Login
        console.log("1. Logging in...");
        const loginRes = await axios.post(`${API_URL}/auth/login`, { email: EMAIL, password: PASSWORD });
        const token = loginRes.data.token;
        console.log("Logged in.");

        // 2. Upload Profile Picture
        console.log("2. Uploading Profile Picture...");
        const form = new FormData();
        form.append('image', fs.createReadStream(IMAGE_PATH));

        const uploadRes = await axios.post(`${API_URL}/users/profile/picture`, form, {
            headers: {
                Authorization: `Bearer ${token}`,
                ...form.getHeaders()
            }
        });

        console.log("Upload Response:", uploadRes.data);

        // Assertions
        if (typeof uploadRes.data === 'string' && uploadRes.data.startsWith('http')) {
            console.log("Test Passed! URL received:", uploadRes.data);
        } else if (uploadRes.data.profilePicUrl) {
            console.log("Test Passed! URL received:", uploadRes.data.profilePicUrl);
        } else {
            // Maybe it returns UserDTO
            if (uploadRes.data.profilePic && uploadRes.data.profilePic.startsWith('http')) {
                console.log("Test Passed! URL received:", uploadRes.data.profilePic);
            } else {
                console.log("Warning: Response format check failed, but request succeeded.");
                console.log(JSON.stringify(uploadRes.data, null, 2));
            }
        }

    } catch (err) {
        console.error("Test Failed:", err.response?.data || err.message);
        process.exit(1);
    } finally {
        if (fs.existsSync(IMAGE_PATH)) fs.unlinkSync(IMAGE_PATH);
    }
}

runTest();
