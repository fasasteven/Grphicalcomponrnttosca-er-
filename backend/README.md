# GeoAttend Backend API Server

This is the companion REST API server for the GeoAttend attendance application. It provides complete backend services for lecturer course creation, real-time class announcements, 2-meter proximity validation using the Haversine formula, and device-locked student attendance tracking.

## Quick Start (Local Run)

1. Navigate to the backend directory:
   ```bash
   cd backend
   ```
2. Install dependencies:
   ```bash
   npm install
   ```
3. Start the server:
   ```bash
   npm start
   ```
   The server runs by default on `http://localhost:4000`.

## Suggested Free/Low-Cost Cloud Hosts

To take this API live immediately without managing a VPS:
1. **Render.com** (Free Web Service tier, auto deploys from GitHub repository)
2. **Railway.app** (1-click Node.js deployment)
3. **Fly.io** (Global edge deployments)
4. **Firebase Cloud Functions** / **Supabase** (Postgres + real-time database)

## Core API Endpoints

- `POST /api/auth/register` — Register a Lecturer or Student with secret passcode & device lock.
- `POST /api/auth/login` — Login with identifier and passcode. Enforces 1 student per phone.
- `GET /api/courses` — List courses offered by faculty.
- `POST /api/courses` — Lecturer creates a new course.
- `POST /api/sessions/start` — Lecturer sets an active class, pins GPS coordinates, sets 2m geofence & generates 6-digit code.
- `GET /api/sessions/active` — Active class sessions for student discovery.
- `POST /api/attendance/verify` — Validates student is within 2.0m, matches biometric & session passcode/barcode.
- `GET /api/attendance/session/:id` — Live attendance roster for lecturer.
- `GET /api/notifications` — Real-time alerts of class sessions.
