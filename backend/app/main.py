import asyncio
from contextlib import asynccontextmanager
from fastapi import FastAPI, HTTPException, WebSocket, WebSocketDisconnect
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, Field
from .agent import run_agent
from .browser import browser_manager
from .config import settings

@asynccontextmanager
async def lifespan(app):
    await browser_manager.start()
    yield
    await browser_manager.stop()

app = FastAPI(title="WebPilot API", version="0.1.0", lifespan=lifespan)
origins = [x.strip() for x in settings.allowed_origins.split(",") if x.strip()]
app.add_middleware(CORSMiddleware, allow_origins=origins or ["*"],
                   allow_credentials=origins != ["*"], allow_methods=["*"], allow_headers=["*"])

class TaskBody(BaseModel):
    instruction: str = Field(min_length=3, max_length=4000)
class ApprovalBody(BaseModel):
    approved: bool

def get_session(sid):
    try: return browser_manager.get(sid)
    except KeyError: raise HTTPException(404, "Session not found.")

@app.get("/health")
async def health():
    return {"ok":True,"browser":browser_manager.browser is not None}

@app.post("/api/sessions")
async def create_session():
    try:
        s = await browser_manager.create()
        return {"session_id":s.session_id,"status":"ready"}
    except Exception as exc:
        raise HTTPException(503, str(exc))

@app.get("/api/sessions/{sid}")
async def state(sid: str):
    s = get_session(sid)
    return {"session_id":sid,"status":s.status,"message":s.message,"task":s.current_task,
            "url":s.page.url,"title":await s.page.title(),"events":s.events[-25:],
            "pending_approvals":[{"id":k,"description":v["description"]} for k,v in s.pending.items()]}

@app.get("/api/sessions/{sid}/screenshot")
async def screenshot(sid: str):
    s = get_session(sid)
    return {"image":await browser_manager.screenshot(s),"url":s.page.url}

@app.post("/api/sessions/{sid}/tasks")
async def start_task(sid: str, body: TaskBody):
    s = get_session(sid)
    if s.task and not s.task.done():
        raise HTTPException(409,"A task is already running.")
    if s.status == "awaiting_approval":
        raise HTTPException(409,"Resolve the pending approval first.")
    s.task = asyncio.create_task(run_agent(s,body.instruction))
    return {"accepted":True,"status":"running"}

@app.post("/api/sessions/{sid}/tasks/stop")
async def stop_task(sid: str):
    s = get_session(sid)
    if s.task and not s.task.done():
        s.task.cancel()
        s.status = "paused"
        s.emit("paused", "Task stopped by user.")
        return {"stopped": True}
    s.status = "paused"
    s.emit("paused", "No active task.")
    return {"stopped": False}

@app.post("/api/sessions/{sid}/approvals/{approval_id}")
async def approval(sid: str, approval_id: str, body: ApprovalBody):
    s = get_session(sid)
    pending = s.pending.pop(approval_id,None)
    if not pending: raise HTTPException(404,"Approval not found or expired.")
    if not body.approved:
        s.status="paused"; s.emit("rejected","You rejected the proposed action.")
        return {"approved":False}
    try:
        result = await browser_manager.act(s,pending["action"])
        s.status="paused"
        s.emit("approved","Approved action executed. Review it, then send a follow-up.",result=result)
        return {"approved":True,"result":result}
    except Exception as exc:
        s.status="error"
        raise HTTPException(400,str(exc))

@app.websocket("/api/sessions/{sid}/stream")
async def stream(ws: WebSocket, sid: str):
    try: s = browser_manager.get(sid)
    except KeyError:
        await ws.close(code=4404); return
    await ws.accept()
    try:
        while True:
            await ws.send_json({"type":"snapshot","image":await browser_manager.screenshot(s),
                "url":s.page.url,"status":s.status,"message":s.message,"events":s.events[-12:]})
            await asyncio.sleep(1.0)
    except WebSocketDisconnect: return
    except Exception:
        try: await ws.close()
        except Exception: pass
