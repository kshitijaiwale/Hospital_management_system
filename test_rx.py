import requests
import json
import uuid
import random

base_url = 'http://localhost:8080/api/v1'

# 1. Login as Admin/Receptionist to register patient
rec_data = {"email": "admin@hospital.com", "password": "admin"}
r_rec = requests.post(f"{base_url}/auth/login", json=rec_data)
rec_token = r_rec.json().get('token')
rec_headers = {'Authorization': f'Bearer {rec_token}'}

print("Registering patient...")
p_data = {
    "name": "Test Rx Patient " + str(random.randint(1,1000)),
    "email": f"testrx{random.randint(1,1000)}@example.com",
    "password": "Password123",
    "phone": "999888" + str(random.randint(1000,9999)),
    "dateOfBirth": "1990-01-01",
    "bloodGroup": "O_POS",
    "address": "123 Test St",
    "emergencyContactName": "Mom",
    "emergencyContactPhone": "1112223333"
}
r = requests.post(f"{base_url}/patients", headers=rec_headers, json=p_data)
print("Reg status:", r.status_code)

# Patient login
plogin_data = {"email": p_data["email"], "password": p_data["password"]}
r_plog = requests.post(f"{base_url}/auth/login", json=plogin_data)
token = r_plog.json().get('token')
headers = {'Authorization': f'Bearer {token}'}


# 2. Login as Doctor to create stuff
d_data = {"email": "doctor@hospital.com", "password": "Password123"}
r_doc = requests.post(f"{base_url}/auth/login", json=d_data)
doc_token = r_doc.json().get('token')
doc_headers = {'Authorization': f'Bearer {doc_token}'}

# Get patient ID
me = requests.get(f"{base_url}/patients/me", headers=headers).json()
pid = me['patientId']

# Doctor creates case
c_data = {"patientId": pid, "title": "Cough", "description": "Bad cough"}
case_res = requests.post(f"{base_url}/treatment-cases", headers=doc_headers, json=c_data)
print("Create case status:", case_res.status_code, case_res.text)
case = case_res.json()
cid = case['caseId']

# Doctor creates consultation
cons_data = {"treatmentCaseId": cid, "symptoms": "cough", "diagnosis": "cold", "clinicalNotes": "rest"}
cons = requests.post(f"{base_url}/consultations", headers=doc_headers, json=cons_data).json()
consid = cons['consultationId']

# Doctor adds prescription
rx_data = {
    "consultationId": consid,
    "prescriptions": [
        {"medicationName": "Paracetamol", "dosage": "500mg", "frequency": "1-0-1", "duration": "5 days", "instructions": "After meals"}
    ]
}
rx_created = requests.post(f"{base_url}/prescriptions", headers=doc_headers, json=rx_data)
print("Doc add rx status:", rx_created.status_code)

# 3. Patient tries to read them
print("\n--- PATIENT READING ---")
c_res = requests.get(f"{base_url}/patients/{pid}/treatment-cases", headers=headers)
print("Cases status:", c_res.status_code)
cases = c_res.json()

if cases:
    for c in cases:
        print("Checking case:", c['caseId'])
        co_res = requests.get(f"{base_url}/treatment-cases/{c['caseId']}/consultations", headers=headers)
        print(" Consults status:", co_res.status_code)
        if co_res.status_code == 200:
            consults = co_res.json()
            for co in consults:
                print(" Checking consult:", co['consultationId'])
                rx_res = requests.get(f"{base_url}/consultations/{co['consultationId']}/prescriptions", headers=headers)
                print(" Rx status:", rx_res.status_code)
                print(" Rx data:", rx_res.json())
        else:
            print(" ", co_res.text)
