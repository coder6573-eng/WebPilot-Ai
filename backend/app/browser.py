import asyncio
import base64
import uuid
from dataclasses import dataclass, field
from typing import Any
from playwright.async_api import async_playwright, Browser, BrowserContext, Page
from .safety import validate_public_url

@dataclass
class BrowserSession:
    session_id: str
    context: BrowserContext
    page: Page
    status: str = "idle"
    message: str = "Ready when you are."
    current_task: str = ""
    events: list[dict[str, Any]] = field(default_factory=list)
    pending: dict[str, dict[str, Any]] = field(default_factory=dict)
    task: asyncio.Task | None = None

    def emit(self, kind: str, message: str, **extra: Any):
        self.message = message
        self.events.append({"type": kind, "message": message, **extra})
        self.events = self.events[-60:]

class BrowserManager:
    def __init__(self):
        self.playwright = None
        self.browser: Browser | None = None
        self.sessions: dict[str, BrowserSession] = {}

    async def start(self):
        self.playwright = await async_playwright().start()
        self.browser = await self.playwright.chromium.launch(
            headless=True, args=["--no-sandbox", "--disable-dev-shm-usage"])

    async def stop(self):
        for session in list(self.sessions.values()):
            await session.context.close()
        if self.browser:
            await self.browser.close()
        if self.playwright:
            await self.playwright.stop()

    async def create(self):
        if not self.browser:
            raise RuntimeError("Chromium is unavailable.")
        sid = str(uuid.uuid4())
        context = await self.browser.new_context(
            viewport={"width": 390, "height": 844}, device_scale_factor=1,
            is_mobile=True, has_touch=True)
        async def guard_request(route):
            target = route.request.url
            try:
                if target.startswith(("http://", "https://")):
                    validate_public_url(target)
                await route.continue_()
            except Exception:
                await route.abort()
        await context.route("**/*", guard_request)
        page = await context.new_page()
        await page.goto("https://example.com", wait_until="domcontentloaded", timeout=30000)
        session = BrowserSession(sid, context, page)
        session.emit("system", "Remote browser ready.", url=page.url)
        self.sessions[sid] = session
        return session

    def get(self, sid: str):
        if sid not in self.sessions:
            raise KeyError("Session not found.")
        return self.sessions[sid]

    async def screenshot(self, session):
        data = await session.page.screenshot(type="jpeg", quality=52, timeout=10000)
        return base64.b64encode(data).decode("ascii")

    async def inspect(self, session):
        return await session.page.evaluate("""() => ({
          title: document.title, url: location.href,
          text: (document.body?.innerText || '').slice(0, 9000),
          elements: Array.from(document.querySelectorAll('a,button,input,textarea,select,[role=button]'))
            .slice(0, 70).map((e,i) => {
              const tag=e.tagName.toLowerCase();
              const esc=(v)=>String(v).replace(/\\/g,'\\\\').replace(/"/g,'\\"');
              let selector=e.id ? '#' + esc(e.id) :
                (e.getAttribute('name') ? tag+'[name="' + esc(e.getAttribute('name')) + '"]' :
                (e.getAttribute('aria-label') ? tag+'[aria-label="' + esc(e.getAttribute('aria-label')) + '"]' :
                tag + ':nth-of-type(' + (Array.from(e.parentElement?.children || []).filter(x=>x.tagName===e.tagName).indexOf(e)+1) + ')'));
              return {i,tag,selector,text:(e.innerText || e.getAttribute('aria-label') || e.getAttribute('placeholder') || '').trim().slice(0,100),
                type:e.getAttribute('type') || '', href:e.tagName==='A' ? e.href : ''};
            })
        })""")

    async def act(self, session, action):
        page = session.page
        kind = action.get("action")
        if kind == "navigate":
            await page.goto(validate_public_url(str(action.get("url", ""))),
                            wait_until="domcontentloaded", timeout=30000)
        elif kind == "click":
            selector = str(action.get("selector", ""))[:400]
            if not selector: raise ValueError("Selector required.")
            await page.locator(selector).first.click(timeout=10000)
        elif kind == "fill":
            selector = str(action.get("selector", ""))[:400]
            if not selector: raise ValueError("Selector required.")
            await page.locator(selector).first.fill(str(action.get("text", ""))[:3000], timeout=10000)
        elif kind == "scroll":
            amount = max(-1000, min(1000, int(action.get("amount", 500))))
            await page.evaluate("(n) => window.scrollBy(0,n)", amount)
        elif kind == "back":
            await page.go_back(wait_until="domcontentloaded", timeout=15000)
        elif kind == "wait":
            await page.wait_for_timeout(900)
        else:
            raise ValueError("Unsupported browser action.")
        return {"url": page.url, "title": await page.title()}

browser_manager = BrowserManager()
