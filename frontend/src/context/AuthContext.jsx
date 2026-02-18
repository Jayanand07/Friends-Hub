import { createContext, useContext, useState, useEffect } from 'react';
import { jwtDecode } from './jwtDecode';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
    const [token, setToken] = useState(localStorage.getItem('token'));
    const [user, setUser] = useState(null);

    useEffect(() => {
        if (token) {
            try {
                const decoded = jwtDecode(token);
                setUser({
                    email: decoded.sub,
                    id: decoded.userId || decoded.id,
                    role: decoded.role
                });
            } catch {
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
