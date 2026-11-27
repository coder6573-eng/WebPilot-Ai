# WebPilot AI

Android-first autonomous browser agent with two separate mobile panels: **Agent** and **Browser**. The remote browser runs Chromium + Playwright on the backend; Android displays its live screenshot stream.

## Architecture
Android (Jetpack Compose) ⇄ FastAPI/WebSocket ⇄ Agent orchestrator ⇄ Playwright Chromium ⇄ websites.

## Backend
Requires Python 3.11+.
```bash
cd backend
python -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
playwright install chromium
cp .env.example .env
# Set AI_API_KEY in backend/.env (keep it off the phone)
uvicorn app.main:app --host 0.0.0.0 --port 8000
```
Defaults target Groq's OpenAI-compatible API. Configure AI_BASE_URL, AI_API_KEY and AI_MODEL. API docs: /docs.

## Docker deployment

Create `backend/.env` from `backend/.env.example`, set your AI provider key, then run:

```bash
docker compose up --build -d
docker compose logs -f webpilot-api
```

Keep this service on a trusted private network unless you add authentication and production hardening. Do not commit `backend/.env`.

## Android
Open `android/` in Android Studio and run app. In Settings, set backend URL. Android emulator default: `http://10.0.2.2:8000`; physical phone: use your server's LAN IP or HTTPS endpoint.

## API
- POST /api/sessions
- GET /api/sessions/{id}
- POST /api/sessions/{id}/tasks
- POST /api/sessions/{id}/approvals/{approval_id}
- GET /api/sessions/{id}/screenshot
- WS /api/sessions/{id}/stream

## Safety and limitations
The agent uses a constrained set of browser actions, not arbitrary shell execution or arbitrary JavaScript. It blocks local/private network destinations and pauses before consequential actions for approval. This is a prototype, not a hardened public multi-tenant service. Website content is untrusted. Review actions before approving them.

The first version streams compressed screenshots over WebSocket (roughly 1 fps), not WebRTC. Browser sessions are in-memory and disappear when the backend restarts. No login/cookies are persisted between sessions.

## APK
GitHub Actions workflow **Android Debug APK** builds and uploads a debug APK on pushes affecting android/ or on manual dispatch.
