"""
Kiro Web Chat - FastAPI + HTML
Chat dengan Kiro AI langsung dari browser HP.
Deploy di Hugging Face Space (Docker SDK).
"""

import os
import httpx
from fastapi import FastAPI, Request
from fastapi.responses import HTMLResponse, JSONResponse
import uvicorn

app = FastAPI()

# ============================================================
# KONFIGURASI
# Set di Hugging Face Space > Settings > Repository Secrets:
#   KIRO_API_KEY = API key dari app.kiro.dev (mulai dengan ksk_)
# ============================================================
KIRO_API_KEY = os.environ.get("KIRO_API_KEY", "")
KIRO_API_BASE = "https://prod.us-east-1.api.kiro.dev"
MODEL = os.environ.get("KIRO_MODEL", "claude-sonnet-4-5")


@app.get("/", response_class=HTMLResponse)
async def home():
    return HTML_PAGE


@app.get("/health")
async def health():
    return {"status": "ok", "kiro_configured": bool(KIRO_API_KEY)}


@app.post("/api/chat")
async def chat(request: Request):
    if not KIRO_API_KEY:
        return JSONResponse({"reply": "⚠️ KIRO_API_KEY belum diset. Buka Settings > Repository Secrets di Space ini."})

    body = await request.json()
    message = body.get("message", "")
    history = body.get("history", [])
    if not message.strip():
        return JSONResponse({"reply": ""})

    messages = []
    for item in history[-10:]:
        role = item.get("role", "user")
        content = item.get("content", "")
        if content:
            messages.append({"role": role, "content": content})
    messages.append({"role": "user", "content": message})

    try:
        async with httpx.AsyncClient(timeout=120.0) as client:
            resp = await client.post(
                f"{KIRO_API_BASE}/v1/chat/completions",
                headers={"Authorization": f"Bearer {KIRO_API_KEY}",
                         "Content-Type": "application/json"},
                json={"model": MODEL, "messages": messages,
                      "max_tokens": 4096, "stream": False},
            )
            if resp.status_code == 200:
                return JSONResponse({"reply": resp.json()["choices"][0]["message"]["content"]})

            resp2 = await client.post(
                f"{KIRO_API_BASE}/v1/messages",
                headers={"x-api-key": KIRO_API_KEY,
                         "anthropic-version": "2023-06-01",
                         "Content-Type": "application/json"},
                json={"model": MODEL, "max_tokens": 4096, "messages": messages},
            )
            if resp2.status_code == 200:
                content = resp2.json().get("content", [{}])
                text = content[0].get("text", "Tidak ada respons.") if content else "Tidak ada respons."
                return JSONResponse({"reply": text})

            return JSONResponse({"reply": f"❌ Kiro API error ({resp.status_code}/{resp2.status_code}). Cek API key kamu."})
    except httpx.TimeoutException:
        return JSONResponse({"reply": "⏱️ Timeout. Coba lagi sebentar."})
    except Exception as e:
        return JSONResponse({"reply": f"❌ Error: {str(e)}"})


HTML_PAGE = """<!DOCTYPE html>
<html lang="id">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
<meta name="theme-color" content="#16213e">
<meta name="apple-mobile-web-app-capable" content="yes">
<meta name="mobile-web-app-capable" content="yes">
<title>Kiro Chat</title>
<link rel="icon" href="data:image/svg+xml,<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 100 100'><text y='.9em' font-size='90'>🦞</text></svg>">
<style>
* { margin:0; padding:0; box-sizing:border-box; -webkit-tap-highlight-color:transparent; }
html, body { height:100%; overflow:hidden; }
body { font-family:-apple-system,BlinkMacSystemFont,"Segoe UI",Roboto,sans-serif; background:#1a1a2e; color:#fff; display:flex; flex-direction:column; height:100vh; height:100dvh; }
#header { background:#16213e; padding:14px 18px; font-size:20px; font-weight:600; display:flex; align-items:center; gap:8px; box-shadow:0 2px 8px rgba(0,0,0,.3); flex-shrink:0; }
#header .clear { margin-left:auto; font-size:13px; font-weight:400; color:#e94560; cursor:pointer; padding:6px 10px; border:1px solid #e94560; border-radius:8px; }
#chat { flex:1; overflow-y:auto; padding:16px 12px; display:flex; flex-direction:column; gap:10px; -webkit-overflow-scrolling:touch; }
.msg { max-width:82%; padding:11px 15px; border-radius:16px; font-size:15px; line-height:1.5; white-space:pre-wrap; word-wrap:break-word; animation:fade .2s ease; }
@keyframes fade { from{opacity:0; transform:translateY(6px);} to{opacity:1; transform:translateY(0);} }
.user { align-self:flex-end; background:#e94560; border-bottom-right-radius:4px; }
.bot { align-self:flex-start; background:#0f3460; border-bottom-left-radius:4px; }
.typing { align-self:flex-start; background:#0f3460; padding:14px 18px; border-radius:16px; border-bottom-left-radius:4px; }
.typing span { display:inline-block; width:8px; height:8px; margin:0 2px; background:#888; border-radius:50%; animation:blink 1.4s infinite both; }
.typing span:nth-child(2){animation-delay:.2s;} .typing span:nth-child(3){animation-delay:.4s;}
@keyframes blink { 0%,80%,100%{opacity:.3;} 40%{opacity:1;} }
#inputArea { background:#16213e; padding:10px 12px; display:flex; gap:8px; align-items:flex-end; flex-shrink:0; }
#input { flex:1; background:#0f3460; color:#fff; border:none; border-radius:14px; padding:12px 16px; font-size:15px; resize:none; max-height:120px; outline:none; font-family:inherit; }
#input::placeholder { color:#667; }
#send { background:#e94560; border:none; color:#fff; width:48px; height:48px; border-radius:50%; font-size:20px; cursor:pointer; flex-shrink:0; display:flex; align-items:center; justify-content:center; }
#send:active { transform:scale(.92); }
#send:disabled { opacity:.5; }
</style>
</head>
<body>
<div id="header">🦞 Kiro Chat <span class="clear" onclick="clearChat()">Bersihkan</span></div>
<div id="chat"></div>
<div id="inputArea">
<textarea id="input" placeholder="Ketik pesan..." rows="1"></textarea>
<button id="send" onclick="sendMessage()">➤</button>
</div>
<script>
const chat = document.getElementById('chat');
const input = document.getElementById('input');
const sendBtn = document.getElementById('send');
let history = JSON.parse(localStorage.getItem('kiro_history') || '[]');

function render() {
  chat.innerHTML = '';
  if (history.length === 0) {
    addBubble("Halo! 👋 Saya Kiro AI.\\nKetik pesanmu untuk mulai!", 'bot', false);
  }
  history.forEach(m => addBubble(m.content, m.role === 'user' ? 'user' : 'bot', false));
  scrollDown();
}

function addBubble(text, cls, save=true) {
  const d = document.createElement('div');
  d.className = 'msg ' + cls;
  d.textContent = text;
  chat.appendChild(d);
  scrollDown();
  return d;
}

function scrollDown() { setTimeout(()=>{ chat.scrollTop = chat.scrollHeight; }, 50); }

function clearChat() {
  history = [];
  localStorage.removeItem('kiro_history');
  render();
}

input.addEventListener('input', () => {
  input.style.height = 'auto';
  input.style.height = Math.min(input.scrollHeight, 120) + 'px';
});

input.addEventListener('keydown', (e) => {
  if (e.key === 'Enter' && !e.shiftKey) { e.preventDefault(); sendMessage(); }
});

async function sendMessage() {
  const msg = input.value.trim();
  if (!msg) return;
  input.value = '';
  input.style.height = 'auto';
  sendBtn.disabled = true;

  if (history.length === 0) chat.innerHTML = '';
  addBubble(msg, 'user');
  history.push({role:'user', content:msg});

  const typing = document.createElement('div');
  typing.className = 'typing';
  typing.innerHTML = '<span></span><span></span><span></span>';
  chat.appendChild(typing);
  scrollDown();

  try {
    const res = await fetch('/api/chat', {
      method:'POST',
      headers:{'Content-Type':'application/json'},
      body: JSON.stringify({message: msg, history: history})
    });
    const data = await res.json();
    typing.remove();
    addBubble(data.reply, 'bot');
    history.push({role:'assistant', content:data.reply});
    localStorage.setItem('kiro_history', JSON.stringify(history));
  } catch (err) {
    typing.remove();
    addBubble('❌ Gagal terhubung: ' + err.message, 'bot');
  }
  sendBtn.disabled = false;
}

render();
</script>
</body>
</html>"""


if __name__ == "__main__":
    port = int(os.environ.get("PORT", 7860))
    uvicorn.run(app, host="0.0.0.0", port=port)
