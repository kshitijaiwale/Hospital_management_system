import React, { createContext, useState, useEffect } from 'react';
import api from '../api/api';

export const AuthContext = createContext();

export const AuthProvider = ({ children }) => {
    const [user, setUser] = useState(null);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        const token = localStorage.getItem('token');
        if (token) {
            try {
                // simple custom decode since we might not want to install jwt-decode if we don't have to
                // but let's assume we decode it manually or just use the token
                const payload = JSON.parse(atob(token.split('.')[1]));
                // Spring security usually puts authorities in a specific claim or we can just fetch the user profile if there was an endpoint
                // Actually the JWT from the backend has the email as sub, and we might need to parse roles
                setUser({
                    email: payload.sub,
                    roles: payload.roles || payload.authorities || [] // Depends on the backend JWT structure
                });
            } catch (e) {
                console.error("Token decode failed", e);
                localStorage.removeItem('token');
            }
        }
        setLoading(false);
    }, []);

    const login = async (email, password) => {
        const res = await api.post('/auth/login', { email, password });
        const token = res.data.token;
        localStorage.setItem('token', token);
        const payload = JSON.parse(atob(token.split('.')[1]));
        setUser({
            email: payload.sub,
            roles: payload.roles || payload.authorities || []
        });
    };

    const logout = () => {
        localStorage.removeItem('token');
        setUser(null);
    };

    return (
        <AuthContext.Provider value={{ user, login, logout, loading }}>
            {children}
        </AuthContext.Provider>
    );
};
