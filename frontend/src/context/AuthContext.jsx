import { createContext, useContext, useState, useEffect } from 'react';
import { decodeToken } from './jwtDecode';
import { getOrCreateIdentity } from '../crypto/e2ee';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
    const [token, setToken] = useState(() => localStorage.getItem('token'));
    const [user, setUser] = useState(null);

    const activeToken = token || localStorage.getItem('token');

    useEffect(() => {
        if (activeToken) {
            const decoded = decodeToken(activeToken);
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
    }, [activeToken]);

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

    const isAuthenticated = !!activeToken;

    return (
        <AuthContext.Provider value={{ token: activeToken, user, isAuthenticated, loginUser, logout }}>
            {children}
        </AuthContext.Provider>
    );
}

export const useAuth = () => useContext(AuthContext);
