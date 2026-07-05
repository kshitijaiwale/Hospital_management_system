"""
Hospital Management System - Full System Test Suite
Tests all 6 flows against the live running server.
"""
import requests
import uuid
import datetime
import json
import time

BASE_URL = "http://localhost:8080/api/v1"
PASS = "✅ PASS"
FAIL = "❌ FAIL"
results = []

def p(title, status, note=""):
    line = f"  {status}  {title}"
    if note:
        line += f" — {note}"
    print(line)
    results.append({"title": title, "status": status, "note": note})

def print_header(title):
    print(f"\n{'='*60}")
    print(f"  {title}")
    print(f"{'='*60}")

def wait_for_server():
    print("⏳ Waiting for server on localhost:8080 ...")
    for _ in range(30):
        try:
            r = requests.get(f"http://localhost:8080/", timeout=2)
            print("🟢 Server is up!\n")
            return
        except Exception:
            time.sleep(2)
    raise Exception("Server did not start in time.")

def login(email, password):
    resp = requests.post(f"{BASE_URL}/auth/login", json={"email": email, "password": password})
    if resp.status_code == 200:
        return resp.json()["token"]
    raise Exception(f"Login failed for {email}: {resp.status_code} {resp.text}")

def register_staff(admin_token, email, name, role):
    resp = requests.post(
        f"{BASE_URL}/users",
        json={"email": email, "password": "password123", "name": name, "role": role},
        headers={"Authorization": f"Bearer {admin_token}"}
    )
    # 201 = created, or already exists
    return resp.status_code in (201, 400)

def test_flow():
    wait_for_server()

    # ── SETUP TOKENS ─────────────────────────────────────────────────
    print_header("SETUP — Acquiring tokens")
    admin_token = login("admin@clinic.com", "admin")
    print(f"  Admin login → OK")

    register_staff(admin_token, "rec1@clinic.com", "Receptionist One", "RECEPTIONIST")
    register_staff(admin_token, "doc1@clinic.com", "Doctor One", "DOCTOR")

    rec_token   = login("rec1@clinic.com", "password123")
    doc_token   = login("doc1@clinic.com", "password123")
    rec_h  = {"Authorization": f"Bearer {rec_token}"}
    doc_h  = {"Authorization": f"Bearer {doc_token}"}
    adm_h  = {"Authorization": f"Bearer {admin_token}"}
    print(f"  Receptionist login → OK")
    print(f"  Doctor login → OK")

    # ══════════════════════════════════════════════════════════════════
    # FLOW 1: COMPLETE PATIENT JOURNEY
    # ══════════════════════════════════════════════════════════════════
    print_header("FLOW 1 — Complete Patient Journey")

    # 1. Register Patient
    pt_email = f"patient_{uuid.uuid4().hex[:8]}@test.com"
    resp = requests.post(f"{BASE_URL}/patients", json={
        "name":        "John Journey",
        "email":       pt_email,
        "password":    "password123",
        "phone":       "9998887776",
        "dateOfBirth": "1990-01-01",
        "bloodGroup":  "O_POS"
    }, headers=rec_h)
    if resp.status_code == 201:
        patient_id = resp.json()["patientId"]
        p("Receptionist registers patient", PASS, f"patientId={patient_id[:8]}...")
    else:
        p("Receptionist registers patient", FAIL, f"{resp.status_code}: {resp.text[:200]}")
        print("FATAL — cannot continue without a patient. Exiting.")
        return

    # 2. Book Appointment
    dt = (datetime.datetime.now() + datetime.timedelta(days=1)).replace(hour=10, minute=0, second=0, microsecond=0)
    resp = requests.post(f"{BASE_URL}/appointments", json={
        "patientId":            patient_id,
        "appointmentDateTime":  dt.isoformat(),
        "durationMinutes":      30,
        "notes":                "Headache follow-up"
    }, headers=rec_h)
    if resp.status_code == 201:
        appt_id = resp.json()["appointmentId"]
        p("Receptionist books appointment", PASS, f"apptId={appt_id[:8]}...")
    else:
        p("Receptionist books appointment", FAIL, f"{resp.status_code}: {resp.text[:200]}")
        appt_id = None

    # 3. Create Treatment Case
    resp = requests.post(f"{BASE_URL}/treatment-cases", json={
        "patientId":  patient_id,
        "title":      "Migraine Treatment",
        "caseType":   "OUTPATIENT",
        "diagnosis":  "Severe Migraine"
    }, headers=doc_h)
    if resp.status_code == 201:
        case_id = resp.json()["treatmentCaseId"]
        p("Doctor creates treatment case", PASS, f"caseId={case_id[:8]}...")
    else:
        p("Doctor creates treatment case", FAIL, f"{resp.status_code}: {resp.text[:200]}")
        case_id = None

    # 4. Record Consultation (also marks appointment COMPLETED)
    cons_payload = {
        "treatmentCaseId": case_id,
        "symptoms":        "Headache, Nausea",
        "clinicalNotes":   "Needs rest and painkillers"
    }
    if appt_id:
        cons_payload["appointmentId"] = appt_id
    resp = requests.post(f"{BASE_URL}/consultations", json=cons_payload, headers=doc_h)
    if resp.status_code == 201:
        cons_id = resp.json()["consultationId"]
        p("Doctor records consultation (auto-completes appointment)", PASS, f"consultationId={cons_id[:8]}...")
    else:
        p("Doctor records consultation", FAIL, f"{resp.status_code}: {resp.text[:200]}")
        cons_id = None

    # Verify appointment is COMPLETED
    if appt_id:
        resp = requests.get(f"{BASE_URL}/appointments/{appt_id}", headers=doc_h)
        if resp.status_code == 200 and resp.json().get("status") == "COMPLETED":
            p("Appointment auto-completed after consultation", PASS)
        else:
            status = resp.json().get("status", "?") if resp.status_code == 200 else resp.status_code
            p("Appointment auto-completed after consultation", FAIL, f"status={status}")
            
    # 5. Doctor adds Prescription
    if cons_id:
        resp = requests.post(f"{BASE_URL}/prescriptions", json={
            "consultationId": cons_id,
            "prescriptions": [
                {
                    "medicationName": "Ibuprofen",
                    "dosage": "400mg",
                    "frequency": "1-0-1",
                    "duration": "5 days",
                    "instructions": "After meals"
                }
            ]
        }, headers=doc_h)
        if resp.status_code == 201:
            p("Doctor adds prescription", PASS)
        else:
            p("Doctor adds prescription", FAIL, f"{resp.status_code}: {resp.text[:200]}")

    # 5. Doctor Uploads Document
    files = {'file': ('lab_report.txt', b'Full blood count results: normal range', 'text/plain')}
    data  = {'patientId': patient_id, 'documentType': 'LAB_REPORT'}
    if cons_id:
        data['consultationId'] = cons_id
    resp = requests.post(f"{BASE_URL}/documents/upload", files=files, data=data, headers=doc_h)
    if resp.status_code == 201:
        doc_id = resp.json()["documentId"]
        p("Doctor uploads lab document", PASS, f"documentId={doc_id[:8]}...")
    else:
        p("Doctor uploads lab document", FAIL, f"{resp.status_code}: {resp.text[:200]}")
        doc_id = None

    # 6. Generate Invoice
    resp = requests.post(f"{BASE_URL}/invoices", json={
        "patientId":  patient_id,
        "sourceType": "APPOINTMENT",
        "sourceId":   appt_id,
        "amount":     1000.00
    }, headers=rec_h)
    if resp.status_code == 201:
        invoice_id = resp.json()["invoiceId"]
        p("Receptionist generates invoice (₹1000)", PASS, f"invoiceId={invoice_id[:8]}...")
    else:
        p("Receptionist generates invoice", FAIL, f"{resp.status_code}: {resp.text[:200]}")
        invoice_id = None

    # 7. Record Full Payment
    if invoice_id:
        resp = requests.post(f"{BASE_URL}/payments", json={
            "invoiceId":   invoice_id,
            "paymentMode": "CARD",
            "amount":      1000.00
        }, headers=rec_h)
        if resp.status_code == 201:
            p("Receptionist records full payment (₹1000)", PASS)
        else:
            p("Receptionist records full payment", FAIL, f"{resp.status_code}: {resp.text[:200]}")

    # 8. Timeline shows everything
    resp = requests.get(f"{BASE_URL}/patients/{patient_id}/timeline", headers=adm_h)
    if resp.status_code == 200:
        events = resp.json().get("events", [])
        types  = [e["eventType"] for e in events]
        expected = {"APPOINTMENT", "TREATMENT_CASE", "CONSULTATION", "DOCUMENT", "INVOICE", "PAYMENT"}
        missing = expected - set(types)
        if not missing:
            p(f"Timeline shows all event types ({len(events)} events)", PASS)
        else:
            p(f"Timeline shows all event types", FAIL, f"Missing: {missing}, Found: {set(types)}")
        # Print the timeline for inspection
        print(f"\n  📋 Timeline ({len(events)} events, newest first):")
        for e in events:
            status_str = f"[{e.get('status','')}]" if e.get('status') else ""
            print(f"    {e['eventDate'][:16]} | {e['eventType']:15} | {e['title']} {status_str}")
    else:
        p("Timeline retrieval", FAIL, f"{resp.status_code}: {resp.text[:200]}")


    # ══════════════════════════════════════════════════════════════════
    # FLOW 2: SECURITY VALIDATION
    # ══════════════════════════════════════════════════════════════════
    print_header("FLOW 2 — Security / Role Enforcement")

    # Doctor POST /payments → 403
    resp = requests.post(f"{BASE_URL}/payments", json={"invoiceId": str(uuid.uuid4()), "paymentMode": "CASH", "amount": 100}, headers=doc_h)
    if resp.status_code == 403:
        p("Doctor POST /payments → 403 Forbidden", PASS)
    else:
        p("Doctor POST /payments → 403 Forbidden", FAIL, f"Got {resp.status_code}")

    # Receptionist POST /consultations → 403
    resp = requests.post(f"{BASE_URL}/consultations", json={"treatmentCaseId": str(uuid.uuid4())}, headers=rec_h)
    if resp.status_code == 403:
        p("Receptionist POST /consultations → 403 Forbidden", PASS)
    else:
        p("Receptionist POST /consultations → 403 Forbidden", FAIL, f"Got {resp.status_code}")

    # Unauthenticated GET /patients → 401
    resp = requests.get(f"{BASE_URL}/patients")
    if resp.status_code in [401, 403]:
        print("  ✅ PASS  Unauthenticated GET /patients → 401/403")
    else:
        print(f"  ❌ FAIL  Unauthenticated GET /patients → 401 Unauthorized — Got {resp.status_code}")
        failed_tests += 1

    # Doctor POST /invoices → 403
    resp = requests.post(f"{BASE_URL}/invoices", json={"patientId": str(uuid.uuid4()), "sourceType": "REGISTRATION", "amount": 100}, headers=doc_h)
    if resp.status_code == 403:
        p("Doctor POST /invoices → 403 Forbidden", PASS)
    else:
        p("Doctor POST /invoices → 403 Forbidden", FAIL, f"Got {resp.status_code}")


    # ══════════════════════════════════════════════════════════════════
    # FLOW 3: APPOINTMENT SLOT CONFLICT
    # ══════════════════════════════════════════════════════════════════
    print_header("FLOW 3 — Appointment Slot Validation")

    # Create a second patient for this test
    pt2_email = f"patient_{uuid.uuid4().hex[:8]}@test.com"
    resp = requests.post(f"{BASE_URL}/patients", json={
        "name": "Alice Slot", "email": pt2_email, "password": "password123",
        "phone": "8887776665", "dateOfBirth": "1985-05-15", "bloodGroup": "A_POS"
    }, headers=rec_h)
    pt2_id = resp.json()["patientId"] if resp.status_code == 201 else None

    if pt2_id:
        # Use a unique day far in the future (based on UUID) to avoid cross-run collisions
        # but keep the TIME fixed (11:00) so the 11:15 overlap is guaranteed
        random_day_offset = 30 + (uuid.uuid4().int % 500)
        slot_dt = (datetime.datetime.now() + datetime.timedelta(days=random_day_offset)).replace(
            hour=11, minute=0, second=0, microsecond=0)

        # Book first appointment at slot_dt (11:00)
        resp1 = requests.post(f"{BASE_URL}/appointments", json={
            "patientId": pt2_id,
            "appointmentDateTime": slot_dt.isoformat(),
            "durationMinutes": 30
        }, headers=rec_h)
        if resp1.status_code == 201:
            p("First appointment at 11:00 AM booked", PASS)
        else:
            p("First appointment at 11:00 AM", FAIL, f"{resp1.status_code}: {resp1.text[:200]}")

        # Book overlapping appointment at 11:15 AM (same patient, within 30 min slot)
        slot_dt_15 = slot_dt.replace(minute=15)
        resp2 = requests.post(f"{BASE_URL}/appointments", json={
            "patientId": pt2_id,
            "appointmentDateTime": slot_dt_15.isoformat(),
            "durationMinutes": 30
        }, headers=rec_h)
        if resp2.status_code in (400, 409):
            p("Overlapping 11:15 AM slot → Conflict rejected", PASS, f"HTTP {resp2.status_code}")
        else:
            p("Overlapping 11:15 AM slot → Conflict rejected", FAIL, f"Got {resp2.status_code} — expected 400/409")


    # ══════════════════════════════════════════════════════════════════
    # FLOW 4: TREATMENT CASE STATE VALIDATION
    # ══════════════════════════════════════════════════════════════════
    print_header("FLOW 4 — Treatment Case State Validation")

    # Create + immediately close a case
    resp = requests.post(f"{BASE_URL}/treatment-cases", json={
        "patientId": patient_id,
        "title":     "Short-lived Case",
        "caseType":  "OUTPATIENT",
        "diagnosis": "None"
    }, headers=doc_h)
    temp_case_id = resp.json()["treatmentCaseId"] if resp.status_code == 201 else None

    if temp_case_id:
        resp = requests.put(f"{BASE_URL}/treatment-cases/{temp_case_id}/close", headers=doc_h)
        if resp.status_code == 200:
            p("Treatment case closed successfully", PASS)
        else:
            p("Treatment case close", FAIL, f"{resp.status_code}: {resp.text[:200]}")

        # Try adding consultation to closed case
        resp = requests.post(f"{BASE_URL}/consultations", json={
            "treatmentCaseId": temp_case_id,
            "symptoms": "Test symptom"
        }, headers=doc_h)
        if resp.status_code in (400, 409, 422):
            p("Consultation on closed case → Error returned", PASS, f"HTTP {resp.status_code}")
        else:
            p("Consultation on closed case → Error returned", FAIL, f"Got {resp.status_code}")


    # ══════════════════════════════════════════════════════════════════
    # FLOW 5: BILLING PARTIAL PAYMENT VALIDATION
    # ══════════════════════════════════════════════════════════════════
    print_header("FLOW 5 — Billing & Partial Payments")

    resp = requests.post(f"{BASE_URL}/invoices", json={
        "patientId":  patient_id,
        "sourceType": "REGISTRATION",
        "amount":     1000.00
    }, headers=rec_h)
    if resp.status_code == 201:
        bill_inv_id = resp.json()["invoiceId"]
        p("Invoice of ₹1000 created", PASS)
    else:
        p("Invoice of ₹1000 created", FAIL, f"{resp.status_code}: {resp.text[:200]}")
        bill_inv_id = None

    if bill_inv_id:
        # Pay ₹400
        requests.post(f"{BASE_URL}/payments", json={
            "invoiceId": bill_inv_id, "paymentMode": "CASH", "amount": 400.00
        }, headers=rec_h)
        inv_data = requests.get(f"{BASE_URL}/invoices/{bill_inv_id}", headers=rec_h).json()
        status_ok = inv_data.get("status") == "PARTIALLY_PAID"
        remaining_ok = float(inv_data.get("remainingAmount", -1)) == 600.0
        if status_ok and remaining_ok:
            p("Pay ₹400 → PARTIALLY_PAID, Remaining=₹600", PASS)
        else:
            p("Pay ₹400 → PARTIALLY_PAID, Remaining=₹600", FAIL,
              f"status={inv_data.get('status')}, remaining={inv_data.get('remainingAmount')}")

        # Pay remaining ₹600
        requests.post(f"{BASE_URL}/payments", json={
            "invoiceId": bill_inv_id, "paymentMode": "UPI", "amount": 600.00
        }, headers=rec_h)
        inv_data = requests.get(f"{BASE_URL}/invoices/{bill_inv_id}", headers=rec_h).json()
        status_ok = inv_data.get("status") == "PAID"
        remaining_ok = float(inv_data.get("remainingAmount", -1)) == 0.0
        if status_ok and remaining_ok:
            p("Pay ₹600 → PAID, Remaining=₹0", PASS)
        else:
            p("Pay ₹600 → PAID, Remaining=₹0", FAIL,
              f"status={inv_data.get('status')}, remaining={inv_data.get('remainingAmount')}")

        # Try to overpay → should fail
        resp = requests.post(f"{BASE_URL}/payments", json={
            "invoiceId": bill_inv_id, "paymentMode": "CASH", "amount": 100.00
        }, headers=rec_h)
        if resp.status_code in (400, 409, 422):
            p("Overpay on PAID invoice → BillingException", PASS, f"HTTP {resp.status_code}")
        else:
            p("Overpay on PAID invoice → BillingException", FAIL, f"Got {resp.status_code}")


    # ══════════════════════════════════════════════════════════════════
    # FLOW 6: REPORTING — TIMELINE CHRONOLOGICAL ORDER
    # ══════════════════════════════════════════════════════════════════
    print_header("FLOW 6 — Reporting / Timeline Validation")

    resp = requests.get(f"{BASE_URL}/patients/{patient_id}/timeline", headers=adm_h)
    if resp.status_code == 200:
        events = resp.json().get("events", [])
        types  = [e["eventType"] for e in events]
        # Check all expected event types are present
        expected_types = {"APPOINTMENT", "TREATMENT_CASE", "CONSULTATION", "DOCUMENT", "INVOICE", "PAYMENT"}
        found_types = set(types)
        missing = expected_types - found_types
        if not missing:
            p(f"All 6 event types present on timeline", PASS, f"found={found_types}")
        else:
            p(f"All event types on timeline", FAIL, f"Missing={missing}, Found={found_types}")

        # Verify descending date order (newest first)
        dates = [e["eventDate"] for e in events if e.get("eventDate")]
        is_sorted = all(dates[i] >= dates[i+1] for i in range(len(dates)-1))
        if is_sorted:
            p("Timeline events in descending chronological order", PASS)
        else:
            p("Timeline events in descending chronological order", FAIL, "Events are NOT sorted")

        print(f"\n  📋 Full Timeline ({len(events)} events):")
        for e in events:
            status_str = f"[{e.get('status','')}]" if e.get('status') else ""
            print(f"    {e['eventDate'][:19]}  {e['eventType']:16} {e['title']:40} {status_str}")
    else:
        p("Timeline retrieval", FAIL, f"{resp.status_code}: {resp.text[:200]}")


    # ══════════════════════════════════════════════════════════════════
    # FLOW 7: PATIENT SELF-SERVICE APIS
    # ══════════════════════════════════════════════════════════════════
    print_header("FLOW 7 — Patient Self-Service API")

    # Patient Login
    try:
        pt_token = login(pt_email, "password123")
        p("Patient login", PASS)
    except Exception as e:
        p("Patient login", FAIL, str(e))
        pt_token = None

    if pt_token:
        pt_h = {"Authorization": f"Bearer {pt_token}"}
        
        # 1. Get Me Profile
        resp = requests.get(f"{BASE_URL}/patients/me", headers=pt_h)
        if resp.status_code == 200:
            p("Patient GET /patients/me", PASS)
        else:
            p("Patient GET /patients/me", FAIL, f"{resp.status_code}: {resp.text[:200]}")

        # Patient getting prescriptions
        if case_id and cons_id:
            resp = requests.get(f"{BASE_URL}/consultations/{cons_id}/prescriptions", headers=pt_h)
            if resp.status_code == 200 and len(resp.json()) > 0:
                p("Patient GET /consultations/.../prescriptions", PASS, f"Found {len(resp.json())} prescriptions")
            else:
                p("Patient GET /consultations/.../prescriptions", FAIL, f"Status: {resp.status_code}")

        # 2. View auto-generated APPOINTMENT invoice (should exist due to our auto-billing)
        resp = requests.get(f"{BASE_URL}/patients/{patient_id}/invoices", headers=pt_h)
        if resp.status_code == 200:
            invs = resp.json()
            # There should be the auto-generated 500.00 APPOINTMENT invoice and the manually created 1000.00 APPOINTMENT invoice
            auto_inv = next((i for i in invs if i["totalAmount"] == 500.00 and i["sourceType"] == "APPOINTMENT"), None)
            if auto_inv:
                p("Patient views auto-generated APPOINTMENT invoice (₹500)", PASS)
            else:
                p("Patient views auto-generated APPOINTMENT invoice", FAIL, "Not found in list")
        else:
            p("Patient views invoices", FAIL, f"{resp.status_code}: {resp.text[:200]}")
            
        # 3. Patient tries to access another patient's data (pt2_id) → 403
        resp = requests.get(f"{BASE_URL}/patients/{pt2_id}", headers=pt_h)
        if resp.status_code == 403:
            p("Patient accessing another patient's profile → 403 Forbidden", PASS)
        else:
            p("Patient accessing another patient's profile → 403 Forbidden", FAIL, f"Got {resp.status_code}")

    # ══════════════════════════════════════════════════════════════════
    # SUMMARY
    # ══════════════════════════════════════════════════════════════════
    print_header("SYSTEM TEST SUMMARY")
    passed = [r for r in results if PASS in r["status"]]
    failed = [r for r in results if FAIL in r["status"]]
    print(f"\n  Total : {len(results)}")
    print(f"  Passed: {len(passed)}  ✅")
    print(f"  Failed: {len(failed)}  ❌")
    if failed:
        print(f"\n  Failed Tests:")
        for r in failed:
            print(f"    ❌ {r['title']} — {r['note']}")
    if not failed:
        print(f"\n  🎉 ALL SYSTEM TESTS PASSED — Backend V1 is validated!")
    print()

if __name__ == "__main__":
    test_flow()
