import React, { useState, useEffect, useContext } from 'react';
import api from '../api/api';
import { AuthContext } from '../context/AuthContext';

const TABS = ['Queue', 'Treatment Cases', 'Prescriptions', 'Documents'];

const DoctorDashboard = () => {
    const { logout, user } = useContext(AuthContext);
    const [activeTab, setActiveTab] = useState('Queue');
    const [msgObj, setMsgObj] = useState(null);

    // ── Queue & Consultations ──────────────────────────────────────────────────
    const [queue, setQueue] = useState([]);

    // ── Treatment Cases ────────────────────────────────────────────────────────
    const [patientSearch, setPatientSearch] = useState('');
    const [patients, setPatients] = useState([]);
    const [selectedPatient, setSelectedPatient] = useState(null);
    const [cases, setCases] = useState([]);
    const [newCase, setNewCase] = useState({ patientId: '', title: '', diagnosis: '', caseType: '' });
    
    // ── Consultations (within cases) ───────────────────────────────────────────
    const [selectedCase, setSelectedCase] = useState(null);
    const [consultations, setConsultations] = useState([]);
    const [newConsultation, setNewConsultation] = useState({ treatmentCaseId: '', appointmentId: '', symptoms: '', diagnosis: '', clinicalNotes: '', recommendations: '' });

    // ── Prescriptions ──────────────────────────────────────────────────────────
    const [selectedConsultation, setSelectedConsultation] = useState(null);
    const [prescriptions, setPrescriptions] = useState([]);
    const [newPrescriptionList, setNewPrescriptionList] = useState([{ medicationName: '', dosage: '', frequency: '', duration: '', instructions: '' }]);

    // ── Documents ──────────────────────────────────────────────────────────────
    const [documents, setDocuments] = useState([]);
    const [uploadFile, setUploadFile] = useState(null);
    const [docType, setDocType] = useState('PRESCRIPTION');
    const [docNotes, setDocNotes] = useState('');

    useEffect(() => { fetchQueue(); }, []);

    // ─── Queue fns ─────────────────────────────────────────────────────────────
    const fetchQueue = async () => {
        try { const r = await api.get('/queue/today'); setQueue(r.data); } catch { setQueue([]); }
    };
    const startConsultation = async (entryId) => {
        try { await api.post(`/queue/${entryId}/start-consultation`); fetchQueue(); }
        catch (err) { setMsgObj({ type: 'error', text: err.response?.data?.message || 'Error starting consultation' }); }
    };
    const completeConsultation = async (entryId) => {
        try { await api.post(`/queue/${entryId}/complete-consultation`); fetchQueue(); }
        catch (err) { setMsgObj({ type: 'error', text: err.response?.data?.message || 'Error completing' }); }
    };

    // ─── Patient & Case fns ────────────────────────────────────────────────────
    const searchPatients = async () => {
        try { const r = await api.get(`/patients/search?query=${patientSearch}`); setPatients(r.data); } catch {}
    };
    const fetchCases = async (pid) => {
        try { const r = await api.get(`/patients/${pid}/treatment-cases`); setCases(r.data); } catch { setCases([]); }
    };
    const handleCreateCase = async (e) => {
        e.preventDefault(); setMsgObj(null);
        try {
            await api.post('/treatment-cases', newCase);
            setMsgObj({ type: 'success', text: 'Treatment Case created!' });
            setNewCase({ patientId: selectedPatient?.patientId || '', title: '', diagnosis: '', caseType: '' });
            if (selectedPatient) fetchCases(selectedPatient.patientId);
        } catch (err) { setMsgObj({ type: 'error', text: err.response?.data?.message || 'Failed to create case' }); }
    };

    // ─── Consultation fns ──────────────────────────────────────────────────────
    const fetchConsultations = async (caseId) => {
        try { const r = await api.get(`/treatment-cases/${caseId}/consultations`); setConsultations(r.data); } catch { setConsultations([]); }
    };
    const handleCreateConsultation = async (e) => {
        e.preventDefault(); setMsgObj(null);
        try {
            await api.post('/consultations', newConsultation);
            setMsgObj({ type: 'success', text: 'Consultation record added!' });
            setNewConsultation({ treatmentCaseId: selectedCase?.treatmentCaseId || '', appointmentId: '', symptoms: '', diagnosis: '', clinicalNotes: '', recommendations: '' });
            if (selectedCase) fetchConsultations(selectedCase.treatmentCaseId);
        } catch (err) { setMsgObj({ type: 'error', text: err.response?.data?.message || 'Failed to add consultation' }); }
    };

    // ─── Prescription fns ──────────────────────────────────────────────────────
    const fetchPrescriptions = async (consultId) => {
        try { const r = await api.get(`/consultations/${consultId}/prescriptions`); setPrescriptions(r.data); } catch { setPrescriptions([]); }
    };
    const handleAddPrescription = () => {
        setNewPrescriptionList([...newPrescriptionList, { medicationName: '', dosage: '', frequency: '', duration: '', instructions: '' }]);
    };
    const updatePrescriptionRow = (index, field, value) => {
        const list = [...newPrescriptionList];
        list[index][field] = value;
        setNewPrescriptionList(list);
    };
    const removePrescriptionRow = (index) => {
        const list = [...newPrescriptionList];
        list.splice(index, 1);
        setNewPrescriptionList(list);
    };
    const handleSubmitPrescriptions = async (e) => {
        e.preventDefault(); setMsgObj(null);
        try {
            await api.post('/prescriptions', { consultationId: selectedConsultation.consultationId, prescriptions: newPrescriptionList });
            setMsgObj({ type: 'success', text: 'Prescriptions added!' });
            setNewPrescriptionList([{ medicationName: '', dosage: '', frequency: '', duration: '', instructions: '' }]);
            if (selectedConsultation) fetchPrescriptions(selectedConsultation.consultationId);
        } catch (err) { setMsgObj({ type: 'error', text: err.response?.data?.message || 'Failed to save prescriptions' }); }
    };

    // ─── Document fns ──────────────────────────────────────────────────────────
    const fetchDocuments = async (pid) => {
        try { const r = await api.get(`/patients/${pid}/documents`); setDocuments(r.data); } catch { setDocuments([]); }
    };
    const handleUploadDocument = async (e) => {
        e.preventDefault(); setMsgObj(null);
        if (!uploadFile || !selectedPatient) return;
        const formData = new FormData();
        formData.append('file', uploadFile);
        formData.append('patientId', selectedPatient.patientId);
        formData.append('documentType', docType);
        formData.append('notes', docNotes);
        
        try {
            await api.post('/documents/upload', formData, { headers: { 'Content-Type': 'multipart/form-data' }});
            setMsgObj({ type: 'success', text: 'Document uploaded!' });
            setUploadFile(null); setDocNotes('');
            fetchDocuments(selectedPatient.patientId);
        } catch (err) { setMsgObj({ type: 'error', text: err.response?.data?.message || 'Upload failed' }); }
    };
    const handleDownload = async (doc) => {
        try {
            const res = await api.get(`/documents/${doc.documentId}/download`, { responseType: 'blob' });
            const url = window.URL.createObjectURL(new Blob([res.data]));
            const link = document.createElement('a');
            link.href = url;
            link.setAttribute('download', doc.fileName);
            document.body.appendChild(link);
            link.click();
            link.parentNode.removeChild(link);
        } catch { alert('Failed to download'); }
    };

    // ─── Helpers ───────────────────────────────────────────────────────────────
    const msg = (m) => m && <p className={m.type === 'error' ? 'error' : 'success'}>{m.text}</p>;
    const statusBadge = (s) => {
        const colors = { SCHEDULED: '#3b82f6', COMPLETED: '#22c55e', CANCELLED: '#ef4444', WAITING: '#f59e0b', IN_CONSULTATION: '#10b981', ACTIVE: '#3b82f6', CLOSED: '#6b7280' };
        return <span style={{ background: colors[s] || '#6b7280', color: '#fff', padding: '2px 8px', borderRadius: 4, fontSize: '0.75rem' }}>{s}</span>;
    };

    return (
        <div>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' }}>
                <h1>Doctor Dashboard</h1>
                <button onClick={logout} style={{ background: '#ef4444' }}>Logout</button>
            </div>

            {msg(msgObj)}

            <div style={{ display: 'flex', gap: '0.5rem', marginBottom: '1.5rem', borderBottom: '2px solid #e5e7eb' }}>
                {TABS.map(t => (
                    <button key={t} onClick={() => { setActiveTab(t); setMsgObj(null); }}
                        style={{ padding: '0.5rem 1rem', border: 'none', background: activeTab === t ? '#10b981' : 'transparent',
                            color: activeTab === t ? '#fff' : '#374151', borderRadius: '4px 4px 0 0', cursor: 'pointer', fontWeight: activeTab === t ? 600 : 400 }}>
                        {t}
                    </button>
                ))}
            </div>

            {/* ══ QUEUE TAB ══ */}
            {activeTab === 'Queue' && (
                <div className="card">
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                        <h3>Today's Queue</h3>
                        <button onClick={fetchQueue} style={{ background: '#6b7280', fontSize: '0.8rem' }}>🔄 Refresh</button>
                    </div>
                    {queue.length === 0 ? <p style={{ color: '#6b7280' }}>Queue is empty.</p> : (
                        <table>
                            <thead><tr><th>#</th><th>Patient Name</th><th>Check-in Time</th><th>Status</th><th>Actions</th></tr></thead>
                            <tbody>
                                {queue.map(q => (
                                    <tr key={q.queueEntryId}>
                                        <td><strong>{q.queueNumber}</strong></td>
                                        <td>{q.patientName}</td>
                                        <td>{q.checkInTime ? new Date(q.checkInTime).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) : '—'}</td>
                                        <td>{statusBadge(q.status)}</td>
                                        <td>
                                            {q.status === 'WAITING' && <button onClick={() => startConsultation(q.queueEntryId)} style={{ fontSize: '0.7rem', padding: '0.2rem 0.4rem', background: '#2563eb' }}>▶ Start Consultation</button>}
                                            {q.status === 'IN_CONSULTATION' && <button onClick={() => completeConsultation(q.queueEntryId)} style={{ fontSize: '0.7rem', padding: '0.2rem 0.4rem', background: '#059669' }}>✓ Complete</button>}
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    )}
                </div>
            )}

            {/* ══ TREATMENT CASES TAB ══ */}
            {activeTab === 'Treatment Cases' && (
                <div>
                    <div className="card" style={{ marginBottom: '1rem' }}>
                        <h3>Select Patient</h3>
                        <div style={{ display: 'flex', gap: '0.5rem', marginBottom: '1rem' }}>
                            <input type="text" placeholder="Search by name or email..." value={patientSearch}
                                onChange={e => setPatientSearch(e.target.value)}
                                onKeyDown={e => e.key === 'Enter' && searchPatients()} style={{ flex: 1 }} />
                            <button onClick={searchPatients}>Search</button>
                        </div>
                        {patients.length > 0 && (
                            <table>
                                <thead><tr><th>Name</th><th>Email</th><th>Actions</th></tr></thead>
                                <tbody>
                                    {patients.map(p => (
                                        <tr key={p.patientId} style={{ background: selectedPatient?.patientId === p.patientId ? '#ecfdf5' : '' }}>
                                            <td>{p.name}</td><td>{p.email}</td>
                                            <td>
                                                <button onClick={() => { setSelectedPatient(p); fetchCases(p.patientId); fetchDocuments(p.patientId); }} style={{ fontSize: '0.75rem', padding: '0.25rem 0.5rem' }}>Select</button>
                                            </td>
                                        </tr>
                                    ))}
                                </tbody>
                            </table>
                        )}
                    </div>

                    {selectedPatient && (
                        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem' }}>
                            <div className="card">
                                <h3>Create Treatment Case</h3>
                                <form onSubmit={handleCreateCase} style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
                                    <input type="text" placeholder="Title" value={newCase.title} onChange={e => setNewCase({...newCase, title: e.target.value})} required />
                                    <input type="text" placeholder="Diagnosis (Initial)" value={newCase.diagnosis} onChange={e => setNewCase({...newCase, diagnosis: e.target.value})} />
                                    <input type="text" placeholder="Case Type (e.g. OP, IP)" value={newCase.caseType} onChange={e => setNewCase({...newCase, caseType: e.target.value})} />
                                    <button type="submit" onClick={() => setNewCase({...newCase, patientId: selectedPatient.patientId})}>Create Case</button>
                                </form>
                            </div>
                            <div className="card">
                                <h3>Cases for {selectedPatient.name}</h3>
                                {cases.length === 0 ? <p style={{ color: '#6b7280' }}>No cases found.</p> : (
                                    <table>
                                        <thead><tr><th>Title</th><th>Status</th><th>Date</th><th>Action</th></tr></thead>
                                        <tbody>
                                            {cases.map(c => (
                                                <tr key={c.treatmentCaseId} style={{ background: selectedCase?.treatmentCaseId === c.treatmentCaseId ? '#ecfdf5' : '' }}>
                                                    <td>{c.title}</td><td>{statusBadge(c.status)}</td>
                                                    <td>{new Date(c.openDate).toLocaleDateString()}</td>
                                                    <td><button onClick={() => { setSelectedCase(c); fetchConsultations(c.treatmentCaseId); }} style={{ fontSize: '0.7rem' }}>View</button></td>
                                                </tr>
                                            ))}
                                        </tbody>
                                    </table>
                                )}
                            </div>
                        </div>
                    )}

                    {selectedCase && (
                        <div style={{ marginTop: '1rem', display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem' }}>
                            <div className="card">
                                <h3>Add Consultation to: {selectedCase.title}</h3>
                                <form onSubmit={handleCreateConsultation} style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
                                    <input type="text" placeholder="Appointment ID (Optional)" value={newConsultation.appointmentId} onChange={e => setNewConsultation({...newConsultation, appointmentId: e.target.value})} />
                                    <textarea placeholder="Symptoms" value={newConsultation.symptoms} onChange={e => setNewConsultation({...newConsultation, symptoms: e.target.value})} rows={2} />
                                    <textarea placeholder="Diagnosis" value={newConsultation.diagnosis} onChange={e => setNewConsultation({...newConsultation, diagnosis: e.target.value})} rows={2} />
                                    <textarea placeholder="Clinical Notes" value={newConsultation.clinicalNotes} onChange={e => setNewConsultation({...newConsultation, clinicalNotes: e.target.value})} rows={3} />
                                    <textarea placeholder="Recommendations" value={newConsultation.recommendations} onChange={e => setNewConsultation({...newConsultation, recommendations: e.target.value})} rows={2} />
                                    <button type="submit" onClick={() => setNewConsultation({...newConsultation, treatmentCaseId: selectedCase.treatmentCaseId})}>Save Consultation</button>
                                </form>
                            </div>
                            <div className="card">
                                <h3>Consultation History</h3>
                                {consultations.length === 0 ? <p style={{ color: '#6b7280' }}>No consultations found.</p> : (
                                    <div style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem', maxHeight: '500px', overflowY: 'auto' }}>
                                        {consultations.map(c => (
                                            <div key={c.consultationId} style={{ border: '1px solid #e5e7eb', padding: '0.5rem', borderRadius: 4, background: selectedConsultation?.consultationId === c.consultationId ? '#ecfdf5' : '#f9fafb' }}>
                                                <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '0.5rem' }}>
                                                    <strong>{new Date(c.consultationDate).toLocaleString()}</strong>
                                                    <button onClick={() => { setSelectedConsultation(c); fetchPrescriptions(c.consultationId); setActiveTab('Prescriptions'); }} style={{ fontSize: '0.7rem', padding: '0.2rem 0.5rem', background: '#8b5cf6' }}>Rx Prescribe</button>
                                                </div>
                                                <p style={{ margin: 0, fontSize: '0.8rem' }}><strong>Symptoms:</strong> {c.symptoms}</p>
                                                <p style={{ margin: 0, fontSize: '0.8rem' }}><strong>Notes:</strong> {c.clinicalNotes}</p>
                                            </div>
                                        ))}
                                    </div>
                                )}
                            </div>
                        </div>
                    )}
                </div>
            )}

            {/* ══ PRESCRIPTIONS TAB ══ */}
            {activeTab === 'Prescriptions' && (
                <div>
                    {!selectedConsultation ? (
                        <div className="card"><p>Select a consultation from the Treatment Cases tab to add prescriptions.</p></div>
                    ) : (
                        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem' }}>
                            <div className="card">
                                <h3>Add Prescriptions</h3>
                                <form onSubmit={handleSubmitPrescriptions}>
                                    {newPrescriptionList.map((rx, idx) => (
                                        <div key={idx} style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.5rem', marginBottom: '1rem', padding: '0.5rem', border: '1px solid #e5e7eb', borderRadius: 4 }}>
                                            <input type="text" placeholder="Medication Name*" value={rx.medicationName} onChange={e => updatePrescriptionRow(idx, 'medicationName', e.target.value)} required style={{ gridColumn: 'span 2' }} />
                                            <input type="text" placeholder="Dosage (e.g. 500mg)" value={rx.dosage} onChange={e => updatePrescriptionRow(idx, 'dosage', e.target.value)} />
                                            <input type="text" placeholder="Frequency (e.g. 1-0-1)" value={rx.frequency} onChange={e => updatePrescriptionRow(idx, 'frequency', e.target.value)} />
                                            <input type="text" placeholder="Duration (e.g. 5 days)" value={rx.duration} onChange={e => updatePrescriptionRow(idx, 'duration', e.target.value)} />
                                            <input type="text" placeholder="Instructions (e.g. after food)" value={rx.instructions} onChange={e => updatePrescriptionRow(idx, 'instructions', e.target.value)} />
                                            {newPrescriptionList.length > 1 && <button type="button" onClick={() => removePrescriptionRow(idx)} style={{ gridColumn: 'span 2', background: '#ef4444', padding: '0.2rem' }}>Remove</button>}
                                        </div>
                                    ))}
                                    <div style={{ display: 'flex', gap: '0.5rem' }}>
                                        <button type="button" onClick={handleAddPrescription} style={{ background: '#6b7280', flex: 1 }}>+ Add Another Medication</button>
                                        <button type="submit" style={{ background: '#8b5cf6', flex: 1 }}>Save All Prescriptions</button>
                                    </div>
                                </form>
                            </div>
                            <div className="card">
                                <h3>Existing Prescriptions</h3>
                                {prescriptions.length === 0 ? <p style={{ color: '#6b7280' }}>No prescriptions found.</p> : (
                                    <table>
                                        <thead><tr><th>Medication</th><th>Dosage</th><th>Freq</th><th>Duration</th></tr></thead>
                                        <tbody>
                                            {prescriptions.map(p => (
                                                <tr key={p.prescriptionId}>
                                                    <td>{p.medicationName}</td>
                                                    <td>{p.dosage}</td>
                                                    <td>{p.frequency}</td>
                                                    <td>{p.duration}</td>
                                                </tr>
                                            ))}
                                        </tbody>
                                    </table>
                                )}
                            </div>
                        </div>
                    )}
                </div>
            )}

            {/* ══ DOCUMENTS TAB ══ */}
            {activeTab === 'Documents' && (
                <div>
                    {!selectedPatient ? (
                        <div className="card"><p>Select a patient in the Treatment Cases tab first.</p></div>
                    ) : (
                        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem' }}>
                            <div className="card">
                                <h3>Upload Document for {selectedPatient.name}</h3>
                                <form onSubmit={handleUploadDocument} style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
                                    <input type="file" onChange={e => setUploadFile(e.target.files[0])} required />
                                    <select value={docType} onChange={e => setDocType(e.target.value)}>
                                        {['PRESCRIPTION','LAB_REPORT','SCAN_REPORT','CLINICAL_NOTES','ID_PROOF','OTHER'].map(t => <option key={t} value={t}>{t}</option>)}
                                    </select>
                                    <input type="text" placeholder="Notes/Description" value={docNotes} onChange={e => setDocNotes(e.target.value)} />
                                    <button type="submit" style={{ background: '#059669' }}>Upload Document</button>
                                </form>
                            </div>
                            <div className="card">
                                <h3>Patient Documents</h3>
                                {documents.length === 0 ? <p style={{ color: '#6b7280' }}>No documents found.</p> : (
                                    <table>
                                        <thead><tr><th>File Name</th><th>Type</th><th>Notes</th><th>Action</th></tr></thead>
                                        <tbody>
                                            {documents.map(d => (
                                                <tr key={d.documentId}>
                                                    <td>{d.fileName}</td>
                                                    <td>{d.documentType}</td>
                                                    <td>{d.notes}</td>
                                                    <td><button onClick={() => handleDownload(d)} style={{ fontSize: '0.7rem' }}>Download</button></td>
                                                </tr>
                                            ))}
                                        </tbody>
                                    </table>
                                )}
                            </div>
                        </div>
                    )}
                </div>
            )}
        </div>
    );
};

export default DoctorDashboard;
