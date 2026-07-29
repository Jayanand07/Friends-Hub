import { createContext, useContext, useState, useEffect } from 'react';
import { decodeToken } from './jwtDecode';
import { getOrCreateIdentity } from '../crypto/e2ee';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
    const [token, setToken] = useState(localStorage.getItem('token'));
    const [user, setUser] = useState(null);

    useEffect(() => {
        if (token) {
            const decoded = decodeToken(token);
            if (decoded) {
                setUser({
                    email: decoded.sub,
                    id: decoded.userId || decoded.id,
                    role: decoded.role
                });
                setTimeout(() => {
                    getOrCreateIdentity().catch((err) => {
                        console.error("Failed to initialize E2EE key:", err);
                    });
                }, 500);
            } else {
                // Token invalid or expired — auto logout
                logout();
            }
        }
    }, [token]);

    const loginUser = (jwt, refreshToken) => {
        localStorage.setItem('token', jwt);
        if (refreshToken) {
            localStorage.setItem('refreshToken', refreshToken);
        }
        setToken(jwt);
    };

    const logout = () => {
        localStorage.removeItem('token');
        localStorage.removeItem('refreshToken');
        localStorage.removeItem('user');
        setToken(null);
        setUser(null);
    };

    return (
        <AuthContext.Provider value={{ token, user, isAuthenticated: !!token, loginUser, logout }}>
            {children}
        </AuthContext.Provider>
    );
}

export const useAuth = () => useContext(AuthContext);
