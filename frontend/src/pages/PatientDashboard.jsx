import React, { useState, useEffect } from 'react';
import api from '../api/api';
import { useNavigate } from 'react-router-dom';
import '../index.css';

const PatientDashboard = () => {
    const [patient, setPatient] = useState(null);
    const [activeTab, setActiveTab] = useState('appointments');
    const [appointments, setAppointments] = useState([]);
    const [invoices, setInvoices] = useState([]);
    const [documents, setDocuments] = useState([]);
    const [treatments, setTreatments] = useState([]);
    const [prescriptions, setPrescriptions] = useState([]);  // flat list: { caseTitle, consultation, prescriptions[] }
    const [loadingPrescriptions, setLoadingPrescriptions] = useState(false);
    const [loading, setLoading] = useState(true);

    // Booking Form State
    const [showBookingModal, setShowBookingModal] = useState(false);
    const [bookingDate, setBookingDate] = useState('');
    const [bookingTime, setBookingTime] = useState('');
    const [bookingNotes, setBookingNotes] = useState('');

    const navigate = useNavigate();

    useEffect(() => {
        const fetchProfile = async () => {
            try {
                const res = await api.get('/patients/me');
                setPatient(res.data);
            } catch (err) {
                console.error('Failed to fetch patient profile', err);
                if (err.response?.status === 401 || err.response?.status === 403) {
                    navigate('/login');
                }
            } finally {
                setLoading(false);
            }
        };
        fetchProfile();
    }, [navigate]);

    useEffect(() => {
        if (!patient) return;

        const loadData = async () => {
            try {
                if (activeTab === 'appointments') {
                    const res = await api.get(`/appointments/patient/${patient.patientId}`);
                    setAppointments(res.data);
                } else if (activeTab === 'billing') {
                    const res = await api.get(`/patients/${patient.patientId}/invoices`);
                    setInvoices(res.data);
                } else if (activeTab === 'documents') {
                    const res = await api.get(`/patients/${patient.patientId}/documents`);
                    setDocuments(res.data);
                } else if (activeTab === 'treatments') {
                    const res = await api.get(`/patients/${patient.patientId}/treatment-cases`);
                    setTreatments(res.data);
                } else if (activeTab === 'prescriptions') {
                    setLoadingPrescriptions(true);
                    setPrescriptions([]);
                    try {
                        // Step 1: get all treatment cases
                        const casesRes = await api.get(`/patients/${patient.patientId}/treatment-cases`);
                        const cases = casesRes.data || [];
                        const allGroups = [];
                        // Step 2: for each case, get consultations
                        for (const tc of cases) {
                            try {
                                const consRes = await api.get(`/treatment-cases/${tc.treatmentCaseId}/consultations`);
                                const consultations = consRes.data || [];
                                // Step 3: for each consultation, get prescriptions
                                for (const con of consultations) {
                                    try {
                                        const rxRes = await api.get(`/consultations/${con.consultationId}/prescriptions`);
                                        const rxList = rxRes.data || [];
                                        if (rxList.length > 0) {
                                            allGroups.push({
                                                caseTitle: tc.title,
                                                caseId: tc.treatmentCaseId,
                                                consultationId: con.consultationId,
                                                consultationDate: con.consultationDate,
                                                clinicalNotes: con.clinicalNotes,
                                                prescriptions: rxList,
                                            });
                                        }
                                    } catch { /* skip inaccessible */ }
                                }
                            } catch { /* skip */ }
                        }
                        setPrescriptions(allGroups);
                    } finally {
                        setLoadingPrescriptions(false);
                    }
                }
            } catch (err) {
                console.error(`Failed to load ${activeTab}`, err);
            }
        };
        loadData();
    }, [patient, activeTab]);

    const handleBookAppointment = async (e) => {
        e.preventDefault();
        try {
            const appointmentDateTime = `${bookingDate}T${bookingTime}:00`;
            await api.post('/appointments', {
                patientId: patient.patientId,
                appointmentDateTime,
                notes: bookingNotes
            });
            setShowBookingModal(false);
            setBookingDate('');
            setBookingTime('');
            setBookingNotes('');
            alert('Appointment booked successfully! A fee of ₹500 has been added to your billing. Please pay at the clinic before check-in.');
            // Reload appointments
            const res = await api.get(`/appointments/patient/${patient.patientId}`);
            setAppointments(res.data);
        } catch (err) {
            alert(err.response?.data?.message || 'Failed to book appointment');
        }
    };

    const handleDownloadDocument = async (docId, fileName) => {
        try {
            const res = await api.get(`/documents/${docId}/download`, { responseType: 'blob' });
            const url = window.URL.createObjectURL(new Blob([res.data]));
            const link = document.createElement('a');
            link.href = url;
            link.setAttribute('download', fileName);
            document.body.appendChild(link);
            link.click();
            link.parentNode.removeChild(link);
        } catch (err) {
            console.error('Download failed', err);
            alert('Failed to download document');
        }
    };

    if (loading) return <div className="dashboard">Loading...</div>;
    if (!patient) return <div className="dashboard">Patient profile not found. Please log in again.</div>;

    return (
        <div className="dashboard patient-dashboard">
            <header className="dashboard-header">
                <div className="header-content">
                    <h1>Welcome, {patient.name}</h1>
                    <p className="subtitle">Manage your healthcare journey</p>
                </div>
            </header>

            <nav className="dashboard-nav">
                <button className={`nav-item ${activeTab === 'appointments' ? 'active' : ''}`} onClick={() => setActiveTab('appointments')}>Appointments</button>
                <button className={`nav-item ${activeTab === 'billing' ? 'active' : ''}`} onClick={() => setActiveTab('billing')}>Billing & Invoices</button>
                <button className={`nav-item ${activeTab === 'documents' ? 'active' : ''}`} onClick={() => setActiveTab('documents')}>Medical Documents</button>
                <button className={`nav-item ${activeTab === 'treatments' ? 'active' : ''}`} onClick={() => setActiveTab('treatments')}>Treatment History</button>
                <button className={`nav-item ${activeTab === 'prescriptions' ? 'active' : ''}`} onClick={() => setActiveTab('prescriptions')}>💊 Prescriptions</button>
            </nav>

            <main className="dashboard-content">
                {activeTab === 'appointments' && (
                    <div className="card fade-in">
                        <div className="card-header" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                            <h2>My Appointments</h2>
                            <button className="btn-primary" onClick={() => setShowBookingModal(true)}>Book Appointment</button>
                        </div>
                        {appointments.length === 0 ? <p className="empty-state">No appointments found.</p> : (
                            <table className="data-table">
                                <thead>
                                    <tr>
                                        <th>Date & Time</th>
                                        <th>Status</th>
                                        <th>Duration</th>
                                        <th>Notes</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    {appointments.map(app => (
                                        <tr key={app.appointmentId}>
                                            <td>{new Date(app.appointmentDateTime).toLocaleString()}</td>
                                            <td><span className={`status-badge ${app.status.toLowerCase()}`}>{app.status}</span></td>
                                            <td>{app.durationMinutes} mins</td>
                                            <td>{app.notes || '-'}</td>
                                        </tr>
                                    ))}
                                </tbody>
                            </table>
                        )}
                    </div>
                )}

                {activeTab === 'billing' && (
                    <div className="card fade-in">
                        <h2>Billing & Invoices</h2>
                        <div className="alert alert-warning" style={{ marginBottom: '1rem', padding: '1rem', background: '#fffbeb', color: '#b45309', borderLeft: '4px solid #f59e0b' }}>
                            <strong>Note:</strong> We currently do not support online payments. Please clear all pending dues physically at the clinic reception prior to check-in.
                        </div>
                        {invoices.length === 0 ? <p className="empty-state">No invoices found.</p> : (
                            <table className="data-table">
                                <thead>
                                    <tr>
                                        <th>Date</th>
                                        <th>Type</th>
                                        <th>Total Amount</th>
                                        <th>Paid Amount</th>
                                        <th>Status</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    {invoices.map(inv => (
                                        <tr key={inv.invoiceId}>
                                            <td>{new Date(inv.createdAt).toLocaleDateString()}</td>
                                            <td>{inv.sourceType}</td>
                                            <td>₹{inv.totalAmount}</td>
                                            <td>₹{inv.paidAmount}</td>
                                            <td><span className={`status-badge ${inv.status.toLowerCase()}`}>{inv.status}</span></td>
                                        </tr>
                                    ))}
                                </tbody>
                            </table>
                        )}
                    </div>
                )}

                {activeTab === 'documents' && (
                    <div className="card fade-in">
                        <h2>Medical Documents</h2>
                        {documents.length === 0 ? <p className="empty-state">No documents found.</p> : (
                            <table className="data-table">
                                <thead>
                                    <tr>
                                        <th>Document Type</th>
                                        <th>File Name</th>
                                        <th>Uploaded On</th>
                                        <th>Action</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    {documents.map(doc => (
                                        <tr key={doc.documentId}>
                                            <td>{doc.documentType}</td>
                                            <td>{doc.fileName}</td>
                                            <td>{new Date(doc.uploadedAt).toLocaleString()}</td>
                                            <td>
                                                <button className="btn-secondary" onClick={() => handleDownloadDocument(doc.documentId, doc.fileName)}>
                                                    Download
                                                </button>
                                            </td>
                                        </tr>
                                    ))}
                                </tbody>
                            </table>
                        )}
                    </div>
                )}

                {activeTab === 'treatments' && (
                    <div className="card fade-in">
                        <h2>Treatment History</h2>
                        {treatments.length === 0 ? <p className="empty-state">No treatment records found.</p> : (
                            <div className="treatment-list">
                                {treatments.map(t => (
                                    <div key={t.caseId} className="treatment-card" style={{ padding: '1rem', border: '1px solid #e5e7eb', marginBottom: '1rem', borderRadius: '8px' }}>
                                        <h3>Case: {t.title}</h3>
                                        <p><strong>Status:</strong> <span className={`status-badge ${t.status.toLowerCase()}`}>{t.status}</span></p>
                                        <p><strong>Started:</strong> {new Date(t.createdAt).toLocaleDateString()}</p>
                                        <p><strong>Description:</strong> {t.description}</p>
                                    </div>
                                ))}
                            </div>
                        )}
                    </div>
                )}

                {activeTab === 'prescriptions' && (
                    <div className="card fade-in">
                        <h2>My Prescriptions</h2>
                        {loadingPrescriptions ? (
                            <p className="empty-state">Loading prescriptions...</p>
                        ) : prescriptions.length === 0 ? (
                            <p className="empty-state">No prescriptions found. Prescriptions are added by your doctor after a consultation.</p>
                        ) : (
                            prescriptions.map((group, gi) => (
                                <div key={gi} style={{ marginBottom: '1.5rem', border: '1px solid #e2e8f0', borderRadius: 8, overflow: 'hidden' }}>
                                    <div style={{ background: '#f8fafc', padding: '0.75rem 1rem', borderBottom: '1px solid #e2e8f0' }}>
                                        <strong style={{ color: '#1e293b' }}>📋 {group.caseTitle}</strong>
                                        <span style={{ marginLeft: '1rem', fontSize: '0.8rem', color: '#64748b' }}>
                                            Consultation: {new Date(group.consultationDate).toLocaleDateString()}
                                        </span>
                                        {group.clinicalNotes && (
                                            <p style={{ margin: '0.25rem 0 0', fontSize: '0.8rem', color: '#475569', fontStyle: 'italic' }}>
                                                Doctor's notes: {group.clinicalNotes}
                                            </p>
                                        )}
                                    </div>
                                    <table className="data-table">
                                        <thead>
                                            <tr>
                                                <th>Medication</th>
                                                <th>Dosage</th>
                                                <th>Frequency</th>
                                                <th>Duration</th>
                                                <th>Instructions</th>
                                            </tr>
                                        </thead>
                                        <tbody>
                                            {group.prescriptions.map(rx => (
                                                <tr key={rx.prescriptionId}>
                                                    <td><strong>{rx.medicationName}</strong></td>
                                                    <td>{rx.dosage}</td>
                                                    <td>{rx.frequency}</td>
                                                    <td>{rx.duration}</td>
                                                    <td style={{ color: '#64748b', fontSize: '0.85rem' }}>{rx.instructions || '—'}</td>
                                                </tr>
                                            ))}
                                        </tbody>
                                    </table>
                                </div>
                            ))
                        )}
                    </div>
                )}
            </main>

            {showBookingModal && (
                <div className="modal-overlay">
                    <div className="modal">
                        <h2>Book Appointment</h2>
                        <form onSubmit={handleBookAppointment}>
                            <div className="form-group">
                                <label>Date</label>
                                <input type="date" required value={bookingDate} onChange={e => setBookingDate(e.target.value)} min={new Date().toISOString().split('T')[0]} />
                            </div>
                            <div className="form-group">
                                <label>Time</label>
                                <input type="time" required value={bookingTime} onChange={e => setBookingTime(e.target.value)} />
                            </div>
                            <div className="form-group">
                                <label>Notes</label>
                                <textarea value={bookingNotes} onChange={e => setBookingNotes(e.target.value)} rows="3" placeholder="Any specific reason for visit..."></textarea>
                            </div>
                            <div className="modal-actions">
                                <button type="button" className="btn-secondary" onClick={() => setShowBookingModal(false)}>Cancel</button>
                                <button type="submit" className="btn-primary">Confirm Booking</button>
                            </div>
                        </form>
                    </div>
                </div>
            )}
        </div>
    );
};

export default PatientDashboard;
