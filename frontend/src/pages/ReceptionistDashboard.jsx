import React, { useState, useEffect, useContext } from 'react';
import api from '../api/api';
import { AuthContext } from '../context/AuthContext';

const TABS = ['Patients', 'Appointments', 'Invoices & Payments', 'Queue'];

const ReceptionistDashboard = () => {
    const { logout } = useContext(AuthContext);
    const [activeTab, setActiveTab] = useState('Patients');

    // ── Patients ────────────────────────────────────────────────────────────
    const [patients, setPatients] = useState([]);
    const [patientSearch, setPatientSearch] = useState('');
    const [newPatient, setNewPatient] = useState({ name: '', email: '', password: '', phone: '', dateOfBirth: '', bloodGroup: 'A_POS' });
    const [patientMsg, setPatientMsg] = useState(null);
    const [selectedPatient, setSelectedPatient] = useState(null);

    // ── Appointments ─────────────────────────────────────────────────────────
    const [todayAppts, setTodayAppts] = useState([]);
    const [patientAppts, setPatientAppts] = useState([]);
    const [newAppt, setNewAppt] = useState({ patientId: '', appointmentDateTime: '', durationMinutes: 30, notes: '' });
    const [apptMsg, setApptMsg] = useState(null);

    // ── Invoices & Payments ──────────────────────────────────────────────────
    const [invoices, setInvoices] = useState([]);
    const [newInvoice, setNewInvoice] = useState({ patientId: '', sourceType: 'REGISTRATION', amount: '' });
    const [payInvoiceId, setPayInvoiceId] = useState('');
    const [payAmount, setPayAmount] = useState('');
    const [payMode, setPayMode] = useState('CASH');
    const [billingMsg, setBillingMsg] = useState(null);

    // ── Queue ────────────────────────────────────────────────────────────────
    const [queue, setQueue] = useState([]);
    const [queueApptId, setQueueApptId] = useState('');
    const [queueMsg, setQueueMsg] = useState(null);

    // ─── Effects ─────────────────────────────────────────────────────────────
    useEffect(() => { fetchPatients(); fetchTodayAppts(); fetchQueue(); }, []);

    useEffect(() => {
        if (selectedPatient) {
            fetchPatientAppts(selectedPatient.patientId);
            fetchPatientInvoices(selectedPatient.patientId);
        }
    }, [selectedPatient]);

    // ─── Patient fns ─────────────────────────────────────────────────────────
    const fetchPatients = async () => {
        try { const r = await api.get('/patients/search?query='); setPatients(r.data); } catch { setPatients([]); }
    };
    const searchPatients = async () => {
        try { const r = await api.get(`/patients/search?query=${patientSearch}`); setPatients(r.data); } catch {}
    };
    const handleAddPatient = async (e) => {
        e.preventDefault(); setPatientMsg(null);
        try {
            await api.post('/patients', newPatient);
            setPatientMsg({ type: 'success', text: 'Patient registered!' });
            setNewPatient({ name: '', email: '', password: '', phone: '', dateOfBirth: '', bloodGroup: 'A_POS' });
            fetchPatients();
        } catch (err) {
            setPatientMsg({ type: 'error', text: err.response?.data?.message || 'Failed to register patient' });
        }
    };

    // ─── Appointment fns ──────────────────────────────────────────────────────
    const fetchTodayAppts = async () => {
        try { const r = await api.get('/appointments/today'); setTodayAppts(r.data); } catch { setTodayAppts([]); }
    };
    const fetchPatientAppts = async (pid) => {
        try { const r = await api.get(`/appointments/patient/${pid}`); setPatientAppts(r.data); } catch { setPatientAppts([]); }
    };
    const handleBookAppt = async (e) => {
        e.preventDefault(); setApptMsg(null);
        try {
            await api.post('/appointments', { ...newAppt, appointmentDateTime: newAppt.appointmentDateTime + ':00' });
            setApptMsg({ type: 'success', text: 'Appointment booked!' });
            setNewAppt({ patientId: '', appointmentDateTime: '', durationMinutes: 30, notes: '' });
            fetchTodayAppts();
        } catch (err) {
            setApptMsg({ type: 'error', text: err.response?.data?.message || 'Failed to book appointment' });
        }
    };
    const cancelAppt = async (id) => {
        if (!window.confirm('Cancel this appointment?')) return;
        try { await api.put(`/appointments/${id}/cancel`); fetchTodayAppts(); if (selectedPatient) fetchPatientAppts(selectedPatient.patientId); }
        catch { alert('Failed to cancel'); }
    };
    const markMissed = async (id) => {
        try { await api.put(`/appointments/${id}/missed`); fetchTodayAppts(); if (selectedPatient) fetchPatientAppts(selectedPatient.patientId); }
        catch { alert('Failed'); }
    };

    // ─── Billing fns ──────────────────────────────────────────────────────────
    const fetchPatientInvoices = async (pid) => {
        try { const r = await api.get(`/patients/${pid}/invoices`); setInvoices(r.data); } catch { setInvoices([]); }
    };
    const handleCreateInvoice = async (e) => {
        e.preventDefault(); setBillingMsg(null);
        try {
            await api.post('/invoices', { ...newInvoice, amount: parseFloat(newInvoice.amount) });
            setBillingMsg({ type: 'success', text: 'Invoice created!' });
            setNewInvoice({ patientId: selectedPatient?.patientId || '', sourceType: 'REGISTRATION', amount: '' });
            if (selectedPatient) fetchPatientInvoices(selectedPatient.patientId);
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
            if (selectedPatient) fetchPatientInvoices(selectedPatient.patientId);
        } catch (err) {
            setBillingMsg({ type: 'error', text: err.response?.data?.message || 'Failed to record payment' });
        }
    };

    // ─── Queue fns ────────────────────────────────────────────────────────────
    const fetchQueue = async () => {
        try {
            const r = await api.get('/queue/today');
            setQueue(r.data);
        } catch { setQueue([]); }
    };
    const checkInPatient = async (e) => {
        e.preventDefault(); setQueueMsg(null);
        try {
            await api.post(`/queue/appointments/${queueApptId}/checkin`);
            setQueueMsg({ type: 'success', text: 'Patient checked in and added to queue!' });
            setQueueApptId(''); fetchQueue();
        } catch (err) {
            setQueueMsg({ type: 'error', text: err.response?.data?.message || 'Failed to check in patient' });
        }
    };
    const startConsultation = async (entryId) => {
        try { await api.post(`/queue/${entryId}/start-consultation`); fetchQueue(); }
        catch (err) { setQueueMsg({ type: 'error', text: err.response?.data?.message || 'Failed' }); }
    };
    const completeConsultation = async (entryId) => {
        try { await api.post(`/queue/${entryId}/complete-consultation`); fetchQueue(); }
        catch (err) { setQueueMsg({ type: 'error', text: err.response?.data?.message || 'Failed' }); }
    };
    const markNoShow = async (entryId) => {
        try { await api.post(`/queue/${entryId}/no-show`); fetchQueue(); }
        catch (err) { setQueueMsg({ type: 'error', text: err.response?.data?.message || 'Failed' }); }
    };
    const cancelQueueEntry = async (entryId) => {
        try { await api.post(`/queue/${entryId}/cancel`); fetchQueue(); }
        catch (err) { setQueueMsg({ type: 'error', text: err.response?.data?.message || 'Failed' }); }
    };

    // ─── Helpers ──────────────────────────────────────────────────────────────
    const msg = (m) => m && <p className={m.type === 'error' ? 'error' : 'success'}>{m.text}</p>;
    const statusBadge = (s) => {
        const colors = { SCHEDULED: '#3b82f6', COMPLETED: '#22c55e', CANCELLED: '#ef4444', MISSED: '#f97316', RESCHEDULED: '#a855f7' };
        return <span style={{ background: colors[s] || '#6b7280', color: '#fff', padding: '2px 8px', borderRadius: 4, fontSize: '0.75rem' }}>{s}</span>;
    };

    return (
        <div>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' }}>
                <h1>Receptionist Dashboard</h1>
                <button onClick={logout} style={{ background: '#ef4444' }}>Logout</button>
            </div>

            {/* ── Tab Bar ── */}
            <div style={{ display: 'flex', gap: '0.5rem', marginBottom: '1.5rem', borderBottom: '2px solid #e5e7eb' }}>
                {TABS.map(t => (
                    <button key={t} onClick={() => setActiveTab(t)}
                        style={{ padding: '0.5rem 1rem', border: 'none', background: activeTab === t ? '#1d4ed8' : 'transparent',
                            color: activeTab === t ? '#fff' : '#374151', borderRadius: '4px 4px 0 0', cursor: 'pointer', fontWeight: activeTab === t ? 600 : 400 }}>
                        {t}
                    </button>
                ))}
            </div>

            {/* ══ PATIENTS TAB ══ */}
            {activeTab === 'Patients' && (
                <div>
                    <div className="card">
                        <h3>Register New Patient</h3>
                        {msg(patientMsg)}
                        <form onSubmit={handleAddPatient} style={{ display: 'grid', gridTemplateColumns: 'repeat(3,1fr)', gap: '0.5rem' }}>
                            <input type="text" placeholder="Full Name" value={newPatient.name} onChange={e => setNewPatient({ ...newPatient, name: e.target.value })} required />
                            <input type="text" placeholder="Email" value={newPatient.email} onChange={e => setNewPatient({ ...newPatient, email: e.target.value.trim() })} required />
                            <input type="password" placeholder="Password (min 8 chars)" value={newPatient.password} onChange={e => setNewPatient({ ...newPatient, password: e.target.value })} minLength={8} required />
                            <input type="text" placeholder="Phone" value={newPatient.phone} onChange={e => setNewPatient({ ...newPatient, phone: e.target.value })} required />
                            <input type="date" value={newPatient.dateOfBirth} onChange={e => setNewPatient({ ...newPatient, dateOfBirth: e.target.value })} required />
                            <select value={newPatient.bloodGroup} onChange={e => setNewPatient({ ...newPatient, bloodGroup: e.target.value })}>
                                {['A_POS','A_NEG','B_POS','B_NEG','O_POS','O_NEG','AB_POS','AB_NEG'].map(b => <option key={b} value={b}>{b.replace('_', '')}</option>)}
                            </select>
                            <button type="submit" style={{ gridColumn: 'span 3' }}>Register Patient</button>
                        </form>
                    </div>

                    <div className="card">
                        <h3>Search Patients</h3>
                        <div style={{ display: 'flex', gap: '0.5rem', marginBottom: '1rem' }}>
                            <input type="text" placeholder="Search by name or email..." value={patientSearch}
                                onChange={e => setPatientSearch(e.target.value)}
                                onKeyDown={e => e.key === 'Enter' && searchPatients()} style={{ flex: 1 }} />
                            <button onClick={searchPatients}>Search</button>
                            <button onClick={fetchPatients} style={{ background: '#6b7280' }}>All</button>
                        </div>
                        <table>
                            <thead><tr><th>Name</th><th>Email</th><th>Phone</th><th>Blood</th><th>Actions</th></tr></thead>
                            <tbody>
                                {patients.map(p => (
                                    <tr key={p.patientId} style={{ background: selectedPatient?.patientId === p.patientId ? '#eff6ff' : '' }}>
                                        <td>{p.name}</td>
                                        <td>{p.email}</td>
                                        <td>{p.phone}</td>
                                        <td>{p.bloodGroup}</td>
                                        <td style={{ display: 'flex', gap: '0.25rem' }}>
                                            <button onClick={() => { setSelectedPatient(p); setActiveTab('Appointments'); }} style={{ fontSize: '0.75rem', padding: '0.25rem 0.5rem' }}>Book Appt</button>
                                            <button onClick={() => { setSelectedPatient(p); setActiveTab('Invoices & Payments'); }} style={{ fontSize: '0.75rem', padding: '0.25rem 0.5rem', background: '#059669' }}>Invoice</button>
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>
                </div>
            )}

            {/* ══ APPOINTMENTS TAB ══ */}
            {activeTab === 'Appointments' && (
                <div>
                    <div className="card">
                        <h3>Book Appointment {selectedPatient && <span style={{ color: '#1d4ed8' }}>for {selectedPatient.name}</span>}</h3>
                        {msg(apptMsg)}
                        <form onSubmit={handleBookAppt} style={{ display: 'grid', gridTemplateColumns: 'repeat(2,1fr)', gap: '0.5rem' }}>
                            <input type="text" placeholder="Patient ID" value={newAppt.patientId}
                                onChange={e => setNewAppt({ ...newAppt, patientId: e.target.value })}
                                defaultValue={selectedPatient?.patientId || ''} required />
                            <input type="datetime-local" value={newAppt.appointmentDateTime}
                                onChange={e => setNewAppt({ ...newAppt, appointmentDateTime: e.target.value })} required />
                            <input type="number" placeholder="Duration (minutes)" value={newAppt.durationMinutes}
                                onChange={e => setNewAppt({ ...newAppt, durationMinutes: parseInt(e.target.value) })} min={15} max={120} />
                            <input type="text" placeholder="Notes (optional)" value={newAppt.notes}
                                onChange={e => setNewAppt({ ...newAppt, notes: e.target.value })} />
                            <button type="submit" style={{ gridColumn: 'span 2' }}>Book Appointment</button>
                        </form>
                        {selectedPatient && <button onClick={() => setNewAppt(a => ({ ...a, patientId: selectedPatient.patientId }))}
                            style={{ marginTop: '0.5rem', background: '#7c3aed', fontSize: '0.8rem' }}>
                            Fill Patient ID from selected: {selectedPatient.name}
                        </button>}
                    </div>

                    <div className="card">
                        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                            <h3>Today's Appointments</h3>
                            <button onClick={fetchTodayAppts} style={{ background: '#6b7280', fontSize: '0.8rem' }}>Refresh</button>
                        </div>
                        {todayAppts.length === 0 ? <p style={{ color: '#6b7280' }}>No appointments today.</p> : (
                            <table>
                                <thead><tr><th>Patient</th><th>Time</th><th>Duration</th><th>Status</th><th>Actions</th></tr></thead>
                                <tbody>
                                    {todayAppts.map(a => (
                                        <tr key={a.appointmentId}>
                                            <td>{a.patientName || a.patientId}</td>
                                            <td>{new Date(a.appointmentDateTime).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}</td>
                                            <td>{a.durationMinutes} min</td>
                                            <td>{statusBadge(a.status)}</td>
                                            <td style={{ display: 'flex', gap: '0.25rem' }}>
                                                {a.status === 'SCHEDULED' && <>
                                                    <button onClick={() => cancelAppt(a.appointmentId)} style={{ fontSize: '0.7rem', padding: '0.2rem 0.4rem', background: '#ef4444' }}>Cancel</button>
                                                    <button onClick={() => markMissed(a.appointmentId)} style={{ fontSize: '0.7rem', padding: '0.2rem 0.4rem', background: '#f97316' }}>Missed</button>
                                                    <button onClick={() => { setQueueApptId(a.appointmentId); setActiveTab('Queue'); }} style={{ fontSize: '0.7rem', padding: '0.2rem 0.4rem', background: '#7c3aed' }}>→ Queue</button>
                                                </>}
                                            </td>
                                        </tr>
                                    ))}
                                </tbody>
                            </table>
                        )}
                    </div>

                    {selectedPatient && (
                        <div className="card">
                            <h3>Appointments for {selectedPatient.name}</h3>
                            {patientAppts.length === 0 ? <p style={{ color: '#6b7280' }}>No appointments found.</p> : (
                                <table>
                                    <thead><tr><th>Date/Time</th><th>Duration</th><th>Status</th><th>Notes</th></tr></thead>
                                    <tbody>
                                        {patientAppts.map(a => (
                                            <tr key={a.appointmentId}>
                                                <td>{new Date(a.appointmentDateTime).toLocaleString()}</td>
                                                <td>{a.durationMinutes} min</td>
                                                <td>{statusBadge(a.status)}</td>
                                                <td>{a.notes || '—'}</td>
                                            </tr>
                                        ))}
                                    </tbody>
                                </table>
                            )}
                        </div>
                    )}
                </div>
            )}

            {/* ══ INVOICES & PAYMENTS TAB ══ */}
            {activeTab === 'Invoices & Payments' && (
                <div>
                    {msg(billingMsg)}
                    <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem' }}>
                        <div className="card">
                            <h3>Generate Invoice</h3>
                            <form onSubmit={handleCreateInvoice} style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
                                <input type="text" placeholder="Patient ID" value={newInvoice.patientId}
                                    onChange={e => setNewInvoice({ ...newInvoice, patientId: e.target.value })} required />
                                {selectedPatient && <button type="button" onClick={() => setNewInvoice(i => ({ ...i, patientId: selectedPatient.patientId }))}
                                    style={{ background: '#7c3aed', fontSize: '0.8rem' }}>Use: {selectedPatient.name}</button>}
                                <select value={newInvoice.sourceType} onChange={e => setNewInvoice({ ...newInvoice, sourceType: e.target.value })}>
                                    {['REGISTRATION','APPOINTMENT','TREATMENT_CASE'].map(t => <option key={t} value={t}>{t.replace('_',' ')}</option>)}
                                </select>
                                <input type="number" placeholder="Amount (₹)" value={newInvoice.amount}
                                    onChange={e => setNewInvoice({ ...newInvoice, amount: e.target.value })} min={1} required />
                                <button type="submit">Generate Invoice</button>
                            </form>
                        </div>

                        <div className="card">
                            <h3>Record Payment</h3>
                            <form onSubmit={handlePayment} style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
                                <input type="text" placeholder="Invoice ID" value={payInvoiceId}
                                    onChange={e => setPayInvoiceId(e.target.value)} required />
                                <input type="number" placeholder="Amount (₹)" value={payAmount}
                                    onChange={e => setPayAmount(e.target.value)} min={1} required />
                                <select value={payMode} onChange={e => setPayMode(e.target.value)}>
                                    {['CASH','CARD','UPI','NET_BANKING'].map(m => <option key={m} value={m}>{m.replace('_',' ')}</option>)}
                                </select>
                                <button type="submit" style={{ background: '#059669' }}>Record Payment</button>
                            </form>
                        </div>
                    </div>

                    {selectedPatient && (
                        <div className="card">
                            <h3>Invoices for {selectedPatient.name}</h3>
                            {invoices.length === 0 ? <p style={{ color: '#6b7280' }}>No invoices found.</p> : (
                                <table>
                                    <thead><tr><th>Invoice ID</th><th>Amount</th><th>Status</th><th>Type</th><th>Date</th><th>Action</th></tr></thead>
                                    <tbody>
                                        {invoices.map(inv => (
                                            <tr key={inv.invoiceId}>
                                                <td style={{ fontSize: '0.7rem', fontFamily: 'monospace' }}>{inv.invoiceId}</td>
                                                <td>₹{inv.totalAmount} <span style={{color:'#6b7280',fontSize:'0.75rem'}}>(Paid: ₹{inv.paidAmount} | Due: ₹{inv.remainingAmount})</span></td>
                                                <td>{statusBadge(inv.status)}</td>
                                                <td>{inv.sourceType}</td>
                                                <td>{new Date(inv.createdAt).toLocaleDateString()}</td>
                                                <td><button onClick={() => { setPayInvoiceId(inv.invoiceId); }} style={{fontSize:'0.7rem',padding:'0.2rem 0.5rem',background:'#059669'}}>Pay</button></td>
                                            </tr>
                                        ))}
                                    </tbody>
                                </table>
                            )}
                        </div>
                    )}
                </div>
            )}

            {/* ══ QUEUE TAB ══ */}
            {activeTab === 'Queue' && (
                <div>
                    <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem' }}>
                        <div className="card">
                            <h3>Check-In Patient</h3>
                            {msg(queueMsg)}
                            <form onSubmit={checkInPatient} style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
                                <input type="text" placeholder="Appointment ID" value={queueApptId}
                                    onChange={e => setQueueApptId(e.target.value)} required />
                                <p style={{fontSize:'0.75rem',color:'#6b7280',margin:0}}>Paste the Appointment ID from today's appointments list</p>
                                <button type="submit">✅ Check In & Issue Token</button>
                            </form>
                        </div>
                        <div className="card" style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
                            <h3>Queue Controls</h3>
                            <button onClick={fetchQueue} style={{ background: '#6b7280' }}>🔄 Refresh Queue</button>
                        </div>
                    </div>

                    <div className="card">
                        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                            <h3>Today's Queue</h3>
                            <button onClick={fetchQueue} style={{ background: '#6b7280', fontSize: '0.8rem' }}>🔄 Refresh</button>
                        </div>
                        {msg(queueMsg)}
                        {queue.length === 0 ? <p style={{ color: '#6b7280' }}>Queue is empty.</p> : (
                            <table>
                                <thead><tr><th>#</th><th>Patient</th><th>Check-in</th><th>Status</th><th>Actions</th></tr></thead>
                                <tbody>
                                    {queue.map(q => (
                                        <tr key={q.queueEntryId}>
                                            <td><strong>{q.queueNumber}</strong></td>
                                            <td>{q.patientName || q.appointmentId}</td>
                                            <td>{q.checkInTime ? new Date(q.checkInTime).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) : '—'}</td>
                                            <td>{statusBadge(q.status)}</td>
                                            <td style={{ display: 'flex', gap: '0.25rem', flexWrap: 'wrap' }}>
                                                {q.status === 'WAITING' && <>
                                                    <button onClick={() => startConsultation(q.queueEntryId)} style={{ fontSize: '0.7rem', padding: '0.2rem 0.4rem', background: '#2563eb' }}>▶ Start</button>
                                                    <button onClick={() => markNoShow(q.queueEntryId)} style={{ fontSize: '0.7rem', padding: '0.2rem 0.4rem', background: '#f97316' }}>No-Show</button>
                                                    <button onClick={() => cancelQueueEntry(q.queueEntryId)} style={{ fontSize: '0.7rem', padding: '0.2rem 0.4rem', background: '#ef4444' }}>Cancel</button>
                                                </>}
                                                {q.status === 'IN_CONSULTATION' && <button onClick={() => completeConsultation(q.queueEntryId)} style={{ fontSize: '0.7rem', padding: '0.2rem 0.4rem', background: '#059669' }}>✓ Complete</button>}
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
    );
};

export default ReceptionistDashboard;
