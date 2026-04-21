import { createContext, useContext, useState, useEffect } from 'react';
import { decodeToken } from './jwtDecode';

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
            } else {
                // Token invalid or expired — auto logout
                logout();
            }
        }
    }, [token]);

    const loginUser = (jwt) => {
        localStorage.setItem('token', jwt);
        setToken(jwt);
    };

    const logout = () => {
        localStorage.removeItem('token');
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
