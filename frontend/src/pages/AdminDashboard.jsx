import React, { useState, useEffect, useContext } from 'react';
import api from '../api/api';
import { AuthContext } from '../context/AuthContext';

const TABS = ['Overview', 'Staff', 'Patients', 'Appointments', 'Billing', 'Queue'];

const statusColors = {
    SCHEDULED: '#3b82f6', COMPLETED: '#22c55e', CANCELLED: '#ef4444',
    MISSED: '#f97316', RESCHEDULED: '#a855f7',
    PENDING: '#f59e0b', PARTIALLY_PAID: '#3b82f6', PAID: '#22c55e',
    WAITING: '#3b82f6', IN_CONSULTATION: '#8b5cf6', NO_SHOW: '#f97316',
    ACTIVE: '#22c55e', CLOSED: '#6b7280',
};

const Badge = ({ s }) => (
    <span style={{
        background: statusColors[s] || '#6b7280', color: '#fff',
        padding: '2px 8px', borderRadius: 4, fontSize: '0.72rem', fontWeight: 600
    }}>{s}</span>
);

const Msg = ({ m }) => m ? (
    <p style={{
        padding: '0.5rem 0.75rem', borderRadius: 6, marginBottom: '0.5rem',
        background: m.type === 'error' ? '#fef2f2' : '#f0fdf4',
        color: m.type === 'error' ? '#dc2626' : '#16a34a',
        border: `1px solid ${m.type === 'error' ? '#fecaca' : '#bbf7d0'}`,
        fontSize: '0.875rem'
    }}>{m.text}</p>
) : null;

const StatCard = ({ label, value, color }) => (
    <div style={{
        background: '#fff', borderRadius: 12, padding: '1.25rem 1.5rem',
        boxShadow: '0 1px 4px rgba(0,0,0,0.08)', borderLeft: `4px solid ${color || '#3b82f6'}`
    }}>
        <p style={{ margin: 0, color: '#6b7280', fontSize: '0.8rem', fontWeight: 500, textTransform: 'uppercase', letterSpacing: '0.05em' }}>{label}</p>
        <p style={{ margin: '0.25rem 0 0', fontSize: '2rem', fontWeight: 700, color: '#111827' }}>{value ?? '—'}</p>
    </div>
);

const AdminDashboard = () => {
    const { logout } = useContext(AuthContext);
    const [activeTab, setActiveTab] = useState('Overview');

    // ── Overview ─────────────────────────────────────────────────────────────
    const [stats, setStats] = useState(null);

    // ── Staff ────────────────────────────────────────────────────────────────
    const [staffForm, setStaffForm] = useState({ name: '', email: '', password: '', role: 'DOCTOR' });
    const [staffMsg, setStaffMsg] = useState(null);

    // ── Patients ─────────────────────────────────────────────────────────────
    const [patients, setPatients] = useState([]);
    const [patientSearch, setPatientSearch] = useState('');
    const [selectedPatient, setSelectedPatient] = useState(null);
    const [timeline, setTimeline] = useState([]);
    const [patientMsg, setPatientMsg] = useState(null);

    // ── Appointments ─────────────────────────────────────────────────────────
    const [todayAppts, setTodayAppts] = useState([]);
    const [apptSearch, setApptSearch] = useState('');
    const [apptPatientAppts, setApptPatientAppts] = useState([]);

    // ── Billing ───────────────────────────────────────────────────────────────
    const [billingPatientId, setBillingPatientId] = useState('');
    const [invoices, setInvoices] = useState([]);
    const [newInvoice, setNewInvoice] = useState({ patientId: '', sourceType: 'REGISTRATION', amount: '' });
    const [payInvoiceId, setPayInvoiceId] = useState('');
    const [payAmount, setPayAmount] = useState('');
    const [payMode, setPayMode] = useState('CASH');
    const [billingMsg, setBillingMsg] = useState(null);

    // ── Queue ─────────────────────────────────────────────────────────────────
    const [queue, setQueue] = useState([]);
    const [queueApptId, setQueueApptId] = useState('');
    const [queueMsg, setQueueMsg] = useState(null);

    // ─── Initial load ─────────────────────────────────────────────────────────
    useEffect(() => {
        fetchStats();
        fetchPatients();
        fetchTodayAppts();
        fetchQueue();
    }, []);

    useEffect(() => {
        if (selectedPatient) fetchTimeline(selectedPatient.patientId);
    }, [selectedPatient]);

    // ─── API calls ───────────────────────────────────────────────────────────
    const fetchStats = async () => {
        try { const r = await api.get('/dashboard/admin'); setStats(r.data); } catch { setStats(null); }
    };

    const fetchPatients = async (q = '') => {
        try { const r = await api.get(`/patients/search?query=${q}`); setPatients(r.data); } catch { setPatients([]); }
    };

    const fetchTimeline = async (pid) => {
        try { const r = await api.get(`/patients/${pid}/timeline`); setTimeline(r.data.events || []); } catch { setTimeline([]); }
    };

    const fetchTodayAppts = async () => {
        try { const r = await api.get('/appointments/today'); setTodayAppts(r.data); } catch { setTodayAppts([]); }
    };

    const fetchPatientInvoices = async (pid) => {
        try { const r = await api.get(`/patients/${pid}/invoices`); setInvoices(r.data); } catch { setInvoices([]); }
    };

    const fetchQueue = async () => {
        try { const r = await api.get('/queue/today'); setQueue(r.data); } catch { setQueue([]); }
    };

    const fetchPatientAppts = async (pid) => {
        try { const r = await api.get(`/appointments/patient/${pid}`); setApptPatientAppts(r.data); } catch { setApptPatientAppts([]); }
    };

    // ─── Staff ───────────────────────────────────────────────────────────────
    const handleRegisterStaff = async (e) => {
        e.preventDefault(); setStaffMsg(null);
        try {
            await api.post('/users', staffForm);
            setStaffMsg({ type: 'success', text: `${staffForm.role} "${staffForm.name}" registered successfully!` });
            setStaffForm({ name: '', email: '', password: '', role: 'DOCTOR' });
            fetchStats();
        } catch (err) {
            setStaffMsg({ type: 'error', text: err.response?.data?.message || 'Registration failed' });
        }
    };

    // ─── Billing ─────────────────────────────────────────────────────────────
    const handleCreateInvoice = async (e) => {
        e.preventDefault(); setBillingMsg(null);
        try {
            await api.post('/invoices', { ...newInvoice, amount: parseFloat(newInvoice.amount) });
            setBillingMsg({ type: 'success', text: 'Invoice created!' });
            setNewInvoice({ patientId: billingPatientId, sourceType: 'REGISTRATION', amount: '' });
            if (billingPatientId) fetchPatientInvoices(billingPatientId);
        } catch (err) {
            setBillingMsg({ type: 'error', text: err.response?.data?.message || 'Failed to create invoice' });
        }
    };

    const handlePayment = async (e) => {
        e.preventDefault(); setBillingMsg(null);
        try {
            await api.post('/payments', { invoiceId: payInvoiceId, amount: parseFloat(payAmount), paymentMode: payMode });
            setBillingMsg({ type: 'success', text: 'Payment recorded!' });
            setPayInvoiceId(''); setPayAmount('');
            if (billingPatientId) fetchPatientInvoices(billingPatientId);
        } catch (err) {
            setBillingMsg({ type: 'error', text: err.response?.data?.message || 'Failed to record payment' });
        }
    };

    // ─── Queue ───────────────────────────────────────────────────────────────
    const checkIn = async (e) => {
        e.preventDefault(); setQueueMsg(null);
        try {
            await api.post(`/queue/appointments/${queueApptId}/checkin`);
            setQueueMsg({ type: 'success', text: 'Patient checked in!' });
            setQueueApptId(''); fetchQueue();
        } catch (err) {
            setQueueMsg({ type: 'error', text: err.response?.data?.message || 'Check-in failed' });
        }
    };

    const queueAction = async (url) => {
        try { await api.post(url); fetchQueue(); }
        catch (err) { setQueueMsg({ type: 'error', text: err.response?.data?.message || 'Action failed' }); }
    };

    // ─── Appointment actions ──────────────────────────────────────────────────
    const cancelAppt = async (id) => {
        if (!window.confirm('Cancel this appointment?')) return;
        try { await api.put(`/appointments/${id}/cancel`); fetchTodayAppts(); } catch { alert('Failed'); }
    };
    const markMissed = async (id) => {
        try { await api.put(`/appointments/${id}/missed`); fetchTodayAppts(); } catch { alert('Failed'); }
    };

    // ─── Render ───────────────────────────────────────────────────────────────
    return (
        <div style={{ minHeight: '100vh', background: '#f8fafc', fontFamily: 'Inter, system-ui, sans-serif' }}>
            {/* Header */}
            <div style={{ background: '#1e293b', color: '#fff', padding: '0.875rem 2rem', display: 'flex', justifyContent: 'space-between', alignItems: 'center', boxShadow: '0 2px 8px rgba(0,0,0,0.2)' }}>
                <div>
                    <h1 style={{ margin: 0, fontSize: '1.25rem', fontWeight: 700 }}>🏥 Admin Dashboard</h1>
                    <p style={{ margin: 0, fontSize: '0.75rem', color: '#94a3b8' }}>Hospital Management System — Full Control</p>
                </div>
                <button onClick={logout} style={{ background: '#ef4444', color: '#fff', border: 'none', borderRadius: 6, padding: '0.4rem 1rem', cursor: 'pointer', fontWeight: 600 }}>Logout</button>
            </div>

            {/* Tab Bar */}
            <div style={{ background: '#fff', borderBottom: '1px solid #e2e8f0', padding: '0 2rem', display: 'flex', gap: '0.25rem' }}>
                {TABS.map(t => (
                    <button key={t} onClick={() => setActiveTab(t)} style={{
                        padding: '0.75rem 1.25rem', border: 'none', borderBottom: activeTab === t ? '3px solid #3b82f6' : '3px solid transparent',
                        background: 'none', color: activeTab === t ? '#3b82f6' : '#64748b', cursor: 'pointer',
                        fontWeight: activeTab === t ? 700 : 400, fontSize: '0.9rem', transition: 'color 0.15s'
                    }}>{t}</button>
                ))}
            </div>

            <div style={{ padding: '1.5rem 2rem', maxWidth: 1400, margin: '0 auto' }}>

                {/* ══ OVERVIEW ══ */}
                {activeTab === 'Overview' && (
                    <div>
                        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' }}>
                            <h2 style={{ margin: 0, color: '#1e293b' }}>System Overview</h2>
                            <button onClick={fetchStats} style={{ background: '#64748b', color: '#fff', border: 'none', borderRadius: 6, padding: '0.4rem 0.9rem', cursor: 'pointer' }}>↻ Refresh</button>
                        </div>
                        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(5, 1fr)', gap: '1rem', marginBottom: '1.5rem' }}>
                            <StatCard label="Total Users" value={stats?.totalUsers} color="#6366f1" />
                            <StatCard label="Total Patients" value={stats?.totalPatients} color="#0ea5e9" />
                            <StatCard label="Total Appointments" value={stats?.totalAppointments} color="#f59e0b" />
                            <StatCard label="Treatment Cases" value={stats?.totalTreatmentCases} color="#22c55e" />
                            <StatCard label="Documents" value={stats?.totalDocuments} color="#ec4899" />
                        </div>
                        <div style={{ background: '#fff', borderRadius: 12, padding: '1.25rem', boxShadow: '0 1px 4px rgba(0,0,0,0.08)' }}>
                            <h3 style={{ margin: '0 0 0.75rem', color: '#1e293b' }}>Today's Queue Summary</h3>
                            <div style={{ display: 'flex', gap: '2rem' }}>
                                {['WAITING', 'IN_CONSULTATION', 'COMPLETED', 'NO_SHOW'].map(s => (
                                    <div key={s} style={{ textAlign: 'center' }}>
                                        <p style={{ margin: 0, fontSize: '1.5rem', fontWeight: 700 }}>{queue.filter(q => q.status === s).length}</p>
                                        <Badge s={s} />
                                    </div>
                                ))}
                                <div style={{ textAlign: 'center' }}>
                                    <p style={{ margin: 0, fontSize: '1.5rem', fontWeight: 700 }}>{queue.length}</p>
                                    <span style={{ fontSize: '0.75rem', color: '#6b7280', fontWeight: 600 }}>TOTAL</span>
                                </div>
                            </div>
                        </div>
                    </div>
                )}

                {/* ══ STAFF ══ */}
                {activeTab === 'Staff' && (
                    <div style={{ maxWidth: 520 }}>
                        <h2 style={{ margin: '0 0 1rem', color: '#1e293b' }}>Register Staff Member</h2>
                        <div style={{ background: '#fff', borderRadius: 12, padding: '1.5rem', boxShadow: '0 1px 4px rgba(0,0,0,0.08)' }}>
                            <Msg m={staffMsg} />
                            <form onSubmit={handleRegisterStaff} style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
                                <input placeholder="Full Name" value={staffForm.name} onChange={e => setStaffForm({ ...staffForm, name: e.target.value })} required
                                    style={{ padding: '0.6rem 0.75rem', border: '1px solid #cbd5e1', borderRadius: 6, fontSize: '0.9rem' }} />
                                <input placeholder="Email" type="email" value={staffForm.email} onChange={e => setStaffForm({ ...staffForm, email: e.target.value.trim() })} required
                                    style={{ padding: '0.6rem 0.75rem', border: '1px solid #cbd5e1', borderRadius: 6, fontSize: '0.9rem' }} />
                                <input placeholder="Password (min 8 characters)" type="password" value={staffForm.password} onChange={e => setStaffForm({ ...staffForm, password: e.target.value })} minLength={8} required
                                    style={{ padding: '0.6rem 0.75rem', border: '1px solid #cbd5e1', borderRadius: 6, fontSize: '0.9rem' }} />
                                <select value={staffForm.role} onChange={e => setStaffForm({ ...staffForm, role: e.target.value })}
                                    style={{ padding: '0.6rem 0.75rem', border: '1px solid #cbd5e1', borderRadius: 6, fontSize: '0.9rem', background: '#fff' }}>
                                    <option value="DOCTOR">Doctor</option>
                                    <option value="RECEPTIONIST">Receptionist</option>
                                    <option value="ADMIN">Admin</option>
                                </select>
                                <button type="submit" style={{ background: '#3b82f6', color: '#fff', border: 'none', borderRadius: 6, padding: '0.65rem', fontWeight: 600, cursor: 'pointer', fontSize: '0.9rem' }}>
                                    Register {staffForm.role}
                                </button>
                            </form>
                        </div>
                    </div>
                )}

                {/* ══ PATIENTS ══ */}
                {activeTab === 'Patients' && (
                    <div>
                        <h2 style={{ margin: '0 0 1rem', color: '#1e293b' }}>Patient Management</h2>
                        <Msg m={patientMsg} />
                        {/* Search */}
                        <div style={{ background: '#fff', borderRadius: 12, padding: '1rem 1.25rem', boxShadow: '0 1px 4px rgba(0,0,0,0.08)', marginBottom: '1rem' }}>
                            <div style={{ display: 'flex', gap: '0.5rem', marginBottom: '1rem' }}>
                                <input placeholder="Search by name, email, phone, MRN..." value={patientSearch}
                                    onChange={e => setPatientSearch(e.target.value)}
                                    onKeyDown={e => e.key === 'Enter' && fetchPatients(patientSearch)}
                                    style={{ flex: 1, padding: '0.6rem 0.75rem', border: '1px solid #cbd5e1', borderRadius: 6, fontSize: '0.875rem' }} />
                                <button onClick={() => fetchPatients(patientSearch)} style={{ background: '#3b82f6', color: '#fff', border: 'none', borderRadius: 6, padding: '0.6rem 1rem', cursor: 'pointer', fontWeight: 600 }}>Search</button>
                                <button onClick={() => { setPatientSearch(''); fetchPatients(''); }} style={{ background: '#64748b', color: '#fff', border: 'none', borderRadius: 6, padding: '0.6rem 1rem', cursor: 'pointer' }}>All</button>
                            </div>
                            <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '0.875rem' }}>
                                <thead><tr style={{ borderBottom: '2px solid #e2e8f0' }}>
                                    {['MRN', 'Name', 'Email', 'Phone', 'Blood Group', 'Status', 'Actions'].map(h => (
                                        <th key={h} style={{ padding: '0.5rem 0.75rem', textAlign: 'left', color: '#64748b', fontWeight: 600 }}>{h}</th>
                                    ))}
                                </tr></thead>
                                <tbody>
                                    {patients.map(p => (
                                        <tr key={p.patientId} style={{ borderBottom: '1px solid #f1f5f9', background: selectedPatient?.patientId === p.patientId ? '#eff6ff' : '' }}>
                                            <td style={{ padding: '0.5rem 0.75rem', fontFamily: 'monospace', fontSize: '0.8rem', color: '#6b7280' }}>{p.patientNumber}</td>
                                            <td style={{ padding: '0.5rem 0.75rem', fontWeight: 600 }}>{p.name || p.user?.name}</td>
                                            <td style={{ padding: '0.5rem 0.75rem' }}>{p.email || p.user?.email}</td>
                                            <td style={{ padding: '0.5rem 0.75rem' }}>{p.phone}</td>
                                            <td style={{ padding: '0.5rem 0.75rem' }}>{p.bloodGroup}</td>
                                            <td style={{ padding: '0.5rem 0.75rem' }}><Badge s={p.status} /></td>
                                            <td style={{ padding: '0.5rem 0.75rem' }}>
                                                <button onClick={() => setSelectedPatient(p)}
                                                    style={{ background: '#6366f1', color: '#fff', border: 'none', borderRadius: 4, padding: '0.25rem 0.6rem', cursor: 'pointer', fontSize: '0.75rem', fontWeight: 600 }}>
                                                    View Timeline
                                                </button>
                                            </td>
                                        </tr>
                                    ))}
                                </tbody>
                            </table>
                        </div>

                        {/* Timeline */}
                        {selectedPatient && (
                            <div style={{ background: '#fff', borderRadius: 12, padding: '1.25rem', boxShadow: '0 1px 4px rgba(0,0,0,0.08)' }}>
                                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.75rem' }}>
                                    <h3 style={{ margin: 0 }}>Timeline — {selectedPatient.name || selectedPatient.user?.name}</h3>
                                    <button onClick={() => setSelectedPatient(null)} style={{ background: '#ef4444', color: '#fff', border: 'none', borderRadius: 6, padding: '0.25rem 0.7rem', cursor: 'pointer', fontSize: '0.8rem' }}>✕ Close</button>
                                </div>
                                {timeline.length === 0 ? <p style={{ color: '#6b7280' }}>No events found.</p> : (
                                    <div style={{ borderLeft: '3px solid #e2e8f0', paddingLeft: '1rem' }}>
                                        {timeline.map((e, i) => (
                                            <div key={i} style={{ marginBottom: '0.875rem', position: 'relative' }}>
                                                <div style={{ position: 'absolute', left: -22, top: 4, width: 10, height: 10, borderRadius: '50%', background: statusColors[e.status] || '#3b82f6' }} />
                                                <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', flexWrap: 'wrap' }}>
                                                    <span style={{ fontSize: '0.72rem', color: '#94a3b8', minWidth: 130 }}>{e.eventDate?.slice(0, 16).replace('T', ' ')}</span>
                                                    <span style={{ fontSize: '0.72rem', background: '#f1f5f9', color: '#475569', padding: '1px 6px', borderRadius: 4, fontWeight: 600 }}>{e.eventType}</span>
                                                    <span style={{ fontSize: '0.875rem', fontWeight: 500, color: '#1e293b' }}>{e.title}</span>
                                                    {e.status && <Badge s={e.status} />}
                                                </div>
                                                {e.description && <p style={{ margin: '0.2rem 0 0 142px', fontSize: '0.8rem', color: '#64748b' }}>{e.description}</p>}
                                            </div>
                                        ))}
                                    </div>
                                )}
                            </div>
                        )}
                    </div>
                )}

                {/* ══ APPOINTMENTS ══ */}
                {activeTab === 'Appointments' && (
                    <div>
                        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' }}>
                            <h2 style={{ margin: 0, color: '#1e293b' }}>Appointments</h2>
                            <button onClick={fetchTodayAppts} style={{ background: '#64748b', color: '#fff', border: 'none', borderRadius: 6, padding: '0.4rem 0.9rem', cursor: 'pointer' }}>↻ Refresh</button>
                        </div>

                        {/* Today's */}
                        <div style={{ background: '#fff', borderRadius: 12, padding: '1.25rem', boxShadow: '0 1px 4px rgba(0,0,0,0.08)', marginBottom: '1rem' }}>
                            <h3 style={{ margin: '0 0 0.75rem' }}>Today's Appointments ({todayAppts.length})</h3>
                            {todayAppts.length === 0 ? <p style={{ color: '#6b7280' }}>No appointments today.</p> : (
                                <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '0.875rem' }}>
                                    <thead><tr style={{ borderBottom: '2px solid #e2e8f0' }}>
                                        {['Patient', 'Time', 'Duration', 'Status', 'Notes', 'Actions'].map(h => (
                                            <th key={h} style={{ padding: '0.5rem 0.75rem', textAlign: 'left', color: '#64748b', fontWeight: 600 }}>{h}</th>
                                        ))}
                                    </tr></thead>
                                    <tbody>
                                        {todayAppts.map(a => (
                                            <tr key={a.appointmentId} style={{ borderBottom: '1px solid #f1f5f9' }}>
                                                <td style={{ padding: '0.5rem 0.75rem', fontWeight: 600 }}>{a.patientName || a.patientId}</td>
                                                <td style={{ padding: '0.5rem 0.75rem' }}>{new Date(a.appointmentDateTime).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}</td>
                                                <td style={{ padding: '0.5rem 0.75rem' }}>{a.durationMinutes} min</td>
                                                <td style={{ padding: '0.5rem 0.75rem' }}><Badge s={a.status} /></td>
                                                <td style={{ padding: '0.5rem 0.75rem', color: '#6b7280', fontSize: '0.8rem' }}>{a.notes || '—'}</td>
                                                <td style={{ padding: '0.5rem 0.75rem', display: 'flex', gap: '0.25rem' }}>
                                                    {a.status === 'SCHEDULED' && <>
                                                        <button onClick={() => cancelAppt(a.appointmentId)} style={{ background: '#ef4444', color: '#fff', border: 'none', borderRadius: 4, padding: '0.2rem 0.5rem', cursor: 'pointer', fontSize: '0.75rem' }}>Cancel</button>
                                                        <button onClick={() => markMissed(a.appointmentId)} style={{ background: '#f97316', color: '#fff', border: 'none', borderRadius: 4, padding: '0.2rem 0.5rem', cursor: 'pointer', fontSize: '0.75rem' }}>Missed</button>
                                                        <button onClick={() => { setQueueApptId(a.appointmentId); setActiveTab('Queue'); }} style={{ background: '#8b5cf6', color: '#fff', border: 'none', borderRadius: 4, padding: '0.2rem 0.5rem', cursor: 'pointer', fontSize: '0.75rem' }}>→ Queue</button>
                                                    </>}
                                                </td>
                                            </tr>
                                        ))}
                                    </tbody>
                                </table>
                            )}
                        </div>

                        {/* Patient-specific search */}
                        <div style={{ background: '#fff', borderRadius: 12, padding: '1.25rem', boxShadow: '0 1px 4px rgba(0,0,0,0.08)' }}>
                            <h3 style={{ margin: '0 0 0.75rem' }}>Lookup Appointments by Patient ID</h3>
                            <div style={{ display: 'flex', gap: '0.5rem', marginBottom: '0.75rem' }}>
                                <input placeholder="Paste Patient ID..." value={apptSearch} onChange={e => setApptSearch(e.target.value)}
                                    style={{ flex: 1, padding: '0.6rem 0.75rem', border: '1px solid #cbd5e1', borderRadius: 6, fontSize: '0.875rem' }} />
                                <button onClick={() => fetchPatientAppts(apptSearch)} style={{ background: '#3b82f6', color: '#fff', border: 'none', borderRadius: 6, padding: '0.6rem 1rem', cursor: 'pointer', fontWeight: 600 }}>Load</button>
                            </div>
                            {apptPatientAppts.length > 0 && (
                                <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '0.875rem' }}>
                                    <thead><tr style={{ borderBottom: '2px solid #e2e8f0' }}>
                                        {['Date/Time', 'Duration', 'Status', 'Notes'].map(h => <th key={h} style={{ padding: '0.5rem 0.75rem', textAlign: 'left', color: '#64748b', fontWeight: 600 }}>{h}</th>)}
                                    </tr></thead>
                                    <tbody>
                                        {apptPatientAppts.map(a => (
                                            <tr key={a.appointmentId} style={{ borderBottom: '1px solid #f1f5f9' }}>
                                                <td style={{ padding: '0.5rem 0.75rem' }}>{new Date(a.appointmentDateTime).toLocaleString()}</td>
                                                <td style={{ padding: '0.5rem 0.75rem' }}>{a.durationMinutes} min</td>
                                                <td style={{ padding: '0.5rem 0.75rem' }}><Badge s={a.status} /></td>
                                                <td style={{ padding: '0.5rem 0.75rem', color: '#6b7280' }}>{a.notes || '—'}</td>
                                            </tr>
                                        ))}
                                    </tbody>
                                </table>
                            )}
                        </div>
                    </div>
                )}

                {/* ══ BILLING ══ */}
                {activeTab === 'Billing' && (
                    <div>
                        <h2 style={{ margin: '0 0 1rem', color: '#1e293b' }}>Billing & Payments</h2>
                        <Msg m={billingMsg} />

                        {/* Load patient invoices */}
                        <div style={{ background: '#fff', borderRadius: 12, padding: '1.25rem', boxShadow: '0 1px 4px rgba(0,0,0,0.08)', marginBottom: '1rem' }}>
                            <h3 style={{ margin: '0 0 0.75rem' }}>Load Patient Invoices</h3>
                            <div style={{ display: 'flex', gap: '0.5rem' }}>
                                <input placeholder="Patient ID..." value={billingPatientId} onChange={e => setBillingPatientId(e.target.value)}
                                    style={{ flex: 1, padding: '0.6rem 0.75rem', border: '1px solid #cbd5e1', borderRadius: 6, fontSize: '0.875rem' }} />
                                <button onClick={() => { setNewInvoice(i => ({ ...i, patientId: billingPatientId })); fetchPatientInvoices(billingPatientId); }}
                                    style={{ background: '#3b82f6', color: '#fff', border: 'none', borderRadius: 6, padding: '0.6rem 1rem', cursor: 'pointer', fontWeight: 600 }}>Load</button>
                            </div>
                        </div>

                        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem', marginBottom: '1rem' }}>
                            {/* Create Invoice */}
                            <div style={{ background: '#fff', borderRadius: 12, padding: '1.25rem', boxShadow: '0 1px 4px rgba(0,0,0,0.08)' }}>
                                <h3 style={{ margin: '0 0 0.75rem' }}>Generate Invoice</h3>
                                <form onSubmit={handleCreateInvoice} style={{ display: 'flex', flexDirection: 'column', gap: '0.6rem' }}>
                                    <input placeholder="Patient ID" value={newInvoice.patientId} onChange={e => setNewInvoice({ ...newInvoice, patientId: e.target.value })} required
                                        style={{ padding: '0.6rem', border: '1px solid #cbd5e1', borderRadius: 6, fontSize: '0.875rem' }} />
                                    <select value={newInvoice.sourceType} onChange={e => setNewInvoice({ ...newInvoice, sourceType: e.target.value })}
                                        style={{ padding: '0.6rem', border: '1px solid #cbd5e1', borderRadius: 6, fontSize: '0.875rem', background: '#fff' }}>
                                        {['REGISTRATION', 'APPOINTMENT', 'TREATMENT_CASE'].map(t => <option key={t} value={t}>{t.replace('_', ' ')}</option>)}
                                    </select>
                                    <input placeholder="Amount (₹)" type="number" value={newInvoice.amount} onChange={e => setNewInvoice({ ...newInvoice, amount: e.target.value })} min={1} required
                                        style={{ padding: '0.6rem', border: '1px solid #cbd5e1', borderRadius: 6, fontSize: '0.875rem' }} />
                                    <button type="submit" style={{ background: '#f59e0b', color: '#fff', border: 'none', borderRadius: 6, padding: '0.6rem', fontWeight: 600, cursor: 'pointer' }}>Generate Invoice</button>
                                </form>
                            </div>
                            {/* Record Payment */}
                            <div style={{ background: '#fff', borderRadius: 12, padding: '1.25rem', boxShadow: '0 1px 4px rgba(0,0,0,0.08)' }}>
                                <h3 style={{ margin: '0 0 0.75rem' }}>Record Payment</h3>
                                <form onSubmit={handlePayment} style={{ display: 'flex', flexDirection: 'column', gap: '0.6rem' }}>
                                    <input placeholder="Invoice ID" value={payInvoiceId} onChange={e => setPayInvoiceId(e.target.value)} required
                                        style={{ padding: '0.6rem', border: '1px solid #cbd5e1', borderRadius: 6, fontSize: '0.875rem' }} />
                                    <input placeholder="Amount (₹)" type="number" value={payAmount} onChange={e => setPayAmount(e.target.value)} min={1} required
                                        style={{ padding: '0.6rem', border: '1px solid #cbd5e1', borderRadius: 6, fontSize: '0.875rem' }} />
                                    <select value={payMode} onChange={e => setPayMode(e.target.value)}
                                        style={{ padding: '0.6rem', border: '1px solid #cbd5e1', borderRadius: 6, fontSize: '0.875rem', background: '#fff' }}>
                                        {['CASH', 'CARD', 'UPI', 'NET_BANKING'].map(m => <option key={m} value={m}>{m.replace('_', ' ')}</option>)}
                                    </select>
                                    <button type="submit" style={{ background: '#22c55e', color: '#fff', border: 'none', borderRadius: 6, padding: '0.6rem', fontWeight: 600, cursor: 'pointer' }}>Record Payment</button>
                                </form>
                            </div>
                        </div>

                        {invoices.length > 0 && (
                            <div style={{ background: '#fff', borderRadius: 12, padding: '1.25rem', boxShadow: '0 1px 4px rgba(0,0,0,0.08)' }}>
                                <h3 style={{ margin: '0 0 0.75rem' }}>Invoices</h3>
                                <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '0.875rem' }}>
                                    <thead><tr style={{ borderBottom: '2px solid #e2e8f0' }}>
                                        {['Invoice ID', 'Type', 'Total', 'Paid', 'Due', 'Status', 'Date', 'Action'].map(h => (
                                            <th key={h} style={{ padding: '0.5rem 0.75rem', textAlign: 'left', color: '#64748b', fontWeight: 600 }}>{h}</th>
                                        ))}
                                    </tr></thead>
                                    <tbody>
                                        {invoices.map(inv => (
                                            <tr key={inv.invoiceId} style={{ borderBottom: '1px solid #f1f5f9' }}>
                                                <td style={{ padding: '0.5rem 0.75rem', fontFamily: 'monospace', fontSize: '0.72rem', color: '#6b7280' }}>{inv.invoiceId?.slice(0, 8)}...</td>
                                                <td style={{ padding: '0.5rem 0.75rem' }}>{inv.sourceType}</td>
                                                <td style={{ padding: '0.5rem 0.75rem', fontWeight: 600 }}>₹{inv.totalAmount}</td>
                                                <td style={{ padding: '0.5rem 0.75rem', color: '#22c55e' }}>₹{inv.paidAmount}</td>
                                                <td style={{ padding: '0.5rem 0.75rem', color: inv.remainingAmount > 0 ? '#ef4444' : '#22c55e' }}>₹{inv.remainingAmount}</td>
                                                <td style={{ padding: '0.5rem 0.75rem' }}><Badge s={inv.status} /></td>
                                                <td style={{ padding: '0.5rem 0.75rem', fontSize: '0.8rem' }}>{new Date(inv.createdAt).toLocaleDateString()}</td>
                                                <td style={{ padding: '0.5rem 0.75rem' }}>
                                                    {inv.status !== 'PAID' && (
                                                        <button onClick={() => setPayInvoiceId(inv.invoiceId)}
                                                            style={{ background: '#22c55e', color: '#fff', border: 'none', borderRadius: 4, padding: '0.2rem 0.6rem', cursor: 'pointer', fontSize: '0.75rem', fontWeight: 600 }}>
                                                            Pay
                                                        </button>
                                                    )}
                                                </td>
                                            </tr>
                                        ))}
                                    </tbody>
                                </table>
                            </div>
                        )}
                    </div>
                )}

                {/* ══ QUEUE ══ */}
                {activeTab === 'Queue' && (
                    <div>
                        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' }}>
                            <h2 style={{ margin: 0, color: '#1e293b' }}>Queue Management</h2>
                            <button onClick={fetchQueue} style={{ background: '#64748b', color: '#fff', border: 'none', borderRadius: 6, padding: '0.4rem 0.9rem', cursor: 'pointer' }}>↻ Refresh</button>
                        </div>
                        <Msg m={queueMsg} />

                        <div style={{ background: '#fff', borderRadius: 12, padding: '1.25rem', boxShadow: '0 1px 4px rgba(0,0,0,0.08)', marginBottom: '1rem', maxWidth: 420 }}>
                            <h3 style={{ margin: '0 0 0.75rem' }}>Check-In Patient</h3>
                            <form onSubmit={checkIn} style={{ display: 'flex', flexDirection: 'column', gap: '0.6rem' }}>
                                <input placeholder="Appointment ID" value={queueApptId} onChange={e => setQueueApptId(e.target.value)} required
                                    style={{ padding: '0.6rem', border: '1px solid #cbd5e1', borderRadius: 6, fontSize: '0.875rem' }} />
                                <button type="submit" style={{ background: '#22c55e', color: '#fff', border: 'none', borderRadius: 6, padding: '0.65rem', fontWeight: 600, cursor: 'pointer' }}>✅ Check In & Issue Token</button>
                            </form>
                        </div>

                        <div style={{ background: '#fff', borderRadius: 12, padding: '1.25rem', boxShadow: '0 1px 4px rgba(0,0,0,0.08)' }}>
                            <h3 style={{ margin: '0 0 0.75rem' }}>Today's Queue ({queue.length} entries)</h3>
                            {queue.length === 0 ? <p style={{ color: '#6b7280' }}>Queue is empty.</p> : (
                                <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '0.875rem' }}>
                                    <thead><tr style={{ borderBottom: '2px solid #e2e8f0' }}>
                                        {['Token #', 'Patient', 'Check-In', 'Status', 'Actions'].map(h => (
                                            <th key={h} style={{ padding: '0.5rem 0.75rem', textAlign: 'left', color: '#64748b', fontWeight: 600 }}>{h}</th>
                                        ))}
                                    </tr></thead>
                                    <tbody>
                                        {queue.map(q => (
                                            <tr key={q.queueEntryId} style={{ borderBottom: '1px solid #f1f5f9' }}>
                                                <td style={{ padding: '0.5rem 0.75rem' }}><strong style={{ fontSize: '1.1rem', color: '#3b82f6' }}>{q.queueNumber}</strong></td>
                                                <td style={{ padding: '0.5rem 0.75rem', fontWeight: 600 }}>{q.patientName || q.appointmentId}</td>
                                                <td style={{ padding: '0.5rem 0.75rem', color: '#6b7280' }}>
                                                    {q.checkInTime ? new Date(q.checkInTime).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) : '—'}
                                                </td>
                                                <td style={{ padding: '0.5rem 0.75rem' }}><Badge s={q.status} /></td>
                                                <td style={{ padding: '0.5rem 0.75rem', display: 'flex', gap: '0.25rem', flexWrap: 'wrap' }}>
                                                    {q.status === 'WAITING' && <>
                                                        <button onClick={() => queueAction(`/queue/${q.queueEntryId}/start-consultation`)}
                                                            style={{ background: '#3b82f6', color: '#fff', border: 'none', borderRadius: 4, padding: '0.2rem 0.5rem', cursor: 'pointer', fontSize: '0.75rem' }}>▶ Start</button>
                                                        <button onClick={() => queueAction(`/queue/${q.queueEntryId}/no-show`)}
                                                            style={{ background: '#f97316', color: '#fff', border: 'none', borderRadius: 4, padding: '0.2rem 0.5rem', cursor: 'pointer', fontSize: '0.75rem' }}>No-Show</button>
                                                        <button onClick={() => queueAction(`/queue/${q.queueEntryId}/cancel`)}
                                                            style={{ background: '#ef4444', color: '#fff', border: 'none', borderRadius: 4, padding: '0.2rem 0.5rem', cursor: 'pointer', fontSize: '0.75rem' }}>Cancel</button>
                                                    </>}
                                                    {q.status === 'IN_CONSULTATION' && (
                                                        <button onClick={() => queueAction(`/queue/${q.queueEntryId}/complete-consultation`)}
                                                            style={{ background: '#22c55e', color: '#fff', border: 'none', borderRadius: 4, padding: '0.2rem 0.5rem', cursor: 'pointer', fontSize: '0.75rem' }}>✓ Complete</button>
                                                    )}
                                                </td>
                                            </tr>
                                        ))}
                                    </tbody>
                                </table>
                            )}
                        </div>
                    </div>
                )}

            </div>
        </div>
    );
};

export default AdminDashboard;
