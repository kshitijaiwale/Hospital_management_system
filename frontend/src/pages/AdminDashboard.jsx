import React, { useState } from 'react';
import api from '../api/api';

const AdminDashboard = () => {
    const [staffData, setStaffData] = useState({
        name: '',
        email: '',
        password: '',
        role: 'DOCTOR'
    });
    const [message, setMessage] = useState('');

    const handleRegister = async (e) => {
        e.preventDefault();
        setMessage('');
        try {
            await api.post('/users', staffData);
            setMessage({ type: 'success', text: 'Staff registered successfully!' });
            setStaffData({ name: '', email: '', password: '', role: 'DOCTOR' });
        } catch (err) {
            const detail = err.response?.data?.message
                || err.response?.data?.error
                || JSON.stringify(err.response?.data)
                || 'Error registering staff';
            setMessage({ type: 'error', text: detail });
        }
    };

    return (
        <div>
            <h1>Admin Dashboard</h1>
            <div className="card">
                <h3>Register Staff</h3>
                {message && <p className={message.type === 'error' ? 'error' : 'success'}>{message.text}</p>}
                <form onSubmit={handleRegister}>
                    <input 
                        type="text" 
                        placeholder="Name" 
                        value={staffData.name}
                        onChange={e => setStaffData({...staffData, name: e.target.value})}
                        required
                    />
                    <input 
                        type="text" 
                        placeholder="Email" 
                        value={staffData.email}
                        onChange={e => setStaffData({...staffData, email: e.target.value.trim()})}
                        required
                    />
                    <input 
                        type="password" 
                        placeholder="Password (min 8 characters)" 
                        value={staffData.password}
                        onChange={e => setStaffData({...staffData, password: e.target.value})}
                        minLength={8}
                        required
                    />
                    <select 
                        value={staffData.role}
                        onChange={e => setStaffData({...staffData, role: e.target.value})}
                    >
                        <option value="DOCTOR">Doctor</option>
                        <option value="RECEPTIONIST">Receptionist</option>
                        <option value="ADMIN">Admin</option>
                    </select>
                    <button type="submit">Register</button>
                </form>
            </div>
        </div>
    );
};

export default AdminDashboard;
