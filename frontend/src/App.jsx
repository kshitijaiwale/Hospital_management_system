import React, { useContext } from 'react';
import { Routes, Route, Navigate, useNavigate } from 'react-router-dom';
import { AuthContext } from './context/AuthContext';
import Login from './pages/Login';
import AdminDashboard from './pages/AdminDashboard';
import ReceptionistDashboard from './pages/ReceptionistDashboard';
import DoctorDashboard from './pages/DoctorDashboard';
import './App.css';

const ProtectedRoute = ({ children, requiredRole }) => {
    const { user, loading } = useContext(AuthContext);

    if (loading) return <div>Loading...</div>;
    
    if (!user) {
        return <Navigate to="/" />;
    }

    if (requiredRole && !user.roles.includes(requiredRole)) {
        return <div>Unauthorized</div>;
    }

    return children;
};

const Layout = ({ children }) => {
    const { user, logout } = useContext(AuthContext);
    const navigate = useNavigate();

    const handleLogout = () => {
        logout();
        navigate('/');
    };

    return (
        <div className="layout">
            <header className="header">
                <h2>Hospital Management System</h2>
                {user && (
                    <div className="user-info">
                        <span>{user.email}</span>
                        <button onClick={handleLogout}>Logout</button>
                    </div>
                )}
            </header>
            <main className="main-content">
                {children}
            </main>
        </div>
    );
};

function App() {
    return (
        <Layout>
            <Routes>
                <Route path="/" element={<Login />} />
                <Route path="/admin/*" element={
                    <ProtectedRoute requiredRole="ROLE_ADMIN">
                        <AdminDashboard />
                    </ProtectedRoute>
                } />
                <Route path="/receptionist/*" element={
                    <ProtectedRoute requiredRole="ROLE_RECEPTIONIST">
                        <ReceptionistDashboard />
                    </ProtectedRoute>
                } />
                <Route path="/doctor/*" element={
                    <ProtectedRoute requiredRole="ROLE_DOCTOR">
                        <DoctorDashboard />
                    </ProtectedRoute>
                } />
            </Routes>
        </Layout>
    );
}

export default App;
