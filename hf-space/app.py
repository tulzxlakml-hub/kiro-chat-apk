import os, json, httpx
from fastapi import FastAPI, Request, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse
import uvicorn

app = FastAPI()
app.add_middleware(CORSMiddleware, allow_origins=["*"], allow_methods=["*"], allow_headers=["*"])

KIRO_API_KEY = os.environ.get("KIRO_API_KEY", "")
APP_SECRET = os.environ.get("APP_SECRET", "")
KIRO_API_BASE = "https://prod.us-east-1.api.kiro.dev"

def auth(request: Request):
    token = request.headers.get("Authorization", "").replace("Bearer ", "")
    if APP_SECRET and token != APP_SECRET:
        raise HTTPException(status_code=401, detail="Unauthorized")

@app.get("/")
async def root():
    return {"status": "ok", "service": "kiro-chat-backend"}

@app.get("/health")
async def health():
    return {"status": "ok", "kiro_configured": bool(KIRO_API_KEY)}

@app.post("/chat")
async def chat(request: Request):
    auth(request)
    if not KIRO_API_KEY:
        raise HTTPException(status_code=500, detail="KIRO_API_KEY not set")
    body = await request.json()
    message = body.get("message", "")
    history = body.get("history", [])
    if not message:
        raise HTTPException(status_code=400, detail="Empty message")

    messages = [{"role": m.get("role","user"), "content": m.get("content","")} for m in history]
    messages.append({"role": "user", "content": message})

    try:
        async with httpx.AsyncClient(timeout=120.0) as client:
            resp = await client.post(
                f"{KIRO_API_BASE}/v1/chat/completions",
                headers={"Authorization": f"Bearer {KIRO_API_KEY}", "Content-Type": "application/json"},
                json={"model": "claude-sonnet-4-5", "messages": messages, "max_tokens": 4096, "stream": False}
            )
            if resp.status_code == 200:
                return JSONResponse({"reply": resp.json()["choices"][0]["message"]["content"], "status": "ok"})
            resp2 = await client.post(
                f"{KIRO_API_BASE}/v1/messages",
                headers={"x-api-key": KIRO_API_KEY, "anthropic-version": "2023-06-01", "Content-Type": "application/json"},
                json={"model": "claude-sonnet-4-5", "max_tokens": 4096, "messages": messages}
            )
            if resp2.status_code == 200:
                return JSONResponse({"reply": resp2.json().get("content",[{}])[0].get("text","No response"), "status": "ok"})
            return JSONResponse({"reply": f"API error ({resp.status_code}/{resp2.status_code})", "status": "error"})
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

if __name__ == "__main__":
    uvicorn.run(app, host="0.0.0.0", port=int(os.environ.get("PORT", 7860)))
