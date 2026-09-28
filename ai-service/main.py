"""云观 AI 助手微服务：DeepSeek Function Calling + SSE 流式输出。

事件协议与 Java 版完全一致：trace {text} / delta {text} / done {ok} / error {message}。
"""
import json
import traceback
from typing import Any

import httpx
from fastapi import FastAPI, HTTPException, Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import StreamingResponse

import auth
import ratelimit
import tools
from config import DEEPSEEK_API_KEY
from llm import MAX_ROUNDS, SYSTEM_PROMPT, LlmError, stream_chat

app = FastAPI(title="云观 AI 助手", version="2.0.0")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)


def _sse(event: str, payload: dict[str, Any]) -> str:
    return f"event: {event}\ndata: {json.dumps(payload, ensure_ascii=False)}\n\n"


@app.get("/health")
async def health() -> dict:
    return {"status": "up", "db": "up" if tools.db_up() else "down"}


@app.post("/api/ai/chat/stream")
async def chat_stream(request: Request):
    username = await auth.require_user(request)  # 401 由 HTTPException 直接返回

    body = await request.json()
    question = (body.get("question") or "").strip()
    history = body.get("history") or []
    if not question:
        raise HTTPException(status_code=400, detail="问题不能为空")

    try:
        ratelimit.check(username)
    except ratelimit.RateLimitError as e:
        # 限流以 SSE error 事件返回，前端气泡直接展示原因
        return StreamingResponse(
            iter([_sse("error", {"message": str(e)})]),
            media_type="text/event-stream",
            headers={"Cache-Control": "no-cache", "X-Accel-Buffering": "no"},
        )

    async def event_stream():
        traces: list[str] = []
        try:
            messages: list[dict[str, Any]] = [{"role": "system", "content": llm_system_prompt()}]
            for h in history:
                role = h.get("role")
                if role in ("user", "assistant") and h.get("content"):
                    messages.append({"role": role, "content": h["content"]})
            messages.append({"role": "user", "content": question})

            last_content = ""
            async with httpx.AsyncClient() as client:
                for _ in range(MAX_ROUNDS):
                    content = ""
                    tool_calls: list[dict[str, Any]] = []
                    async for ev in stream_chat(messages, client):
                        if ev["type"] == "delta":
                            content += ev["text"]
                            yield _sse("delta", {"text": ev["text"]})
                        else:
                            content = ev.get("content") or ""
                            tool_calls = ev.get("tool_calls") or []

                    last_content = content
                    if not tool_calls:
                        break

                    messages.append({
                        "role": "assistant",
                        "content": content or None,
                        "tool_calls": tool_calls,
                    })
                    for tc in tool_calls:
                        name = tc["function"].get("name", "")
                        try:
                            args = json.loads(tc["function"].get("arguments") or "{}")
                        except json.JSONDecodeError:
                            args = {}
                        try:
                            result = tools.execute_tool(name, args)
                        except Exception as exc:  # 查询失败交回 LLM 组织话术
                            result = {"error": f"查询失败：{exc}"}
                        trace = tools.describe_tool(name, args)
                        traces.append(trace)
                        yield _sse("trace", {"text": trace})
                        messages.append({
                            "role": "tool",
                            "tool_call_id": tc.get("id", ""),
                            "content": json.dumps(result, ensure_ascii=False),
                        })

            if not last_content:
                yield _sse("error", {"message": "未能生成回答，请换个问法试试"})
            else:
                yield _sse("done", {"ok": True})
        except LlmError as e:
            yield _sse("error", {"message": str(e)})
        except Exception:
            traceback.print_exc()
            yield _sse("error", {"message": "AI 服务暂不可用，请稍后再试"})
        finally:
            tools.log_operation(username, "AI_CHAT", question[:200])

    return StreamingResponse(
        event_stream(),
        media_type="text/event-stream",
        headers={"Cache-Control": "no-cache", "X-Accel-Buffering": "no"},
    )


def llm_system_prompt() -> str:
    from llm import SYSTEM_PROMPT

    return SYSTEM_PROMPT
