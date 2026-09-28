"""DeepSeek 流式客户端：Function Calling 循环的 LLM 侧。

stream_chat 为异步生成器：
- yield {"type": "delta", "text": ...}   内容增量（实时转发给前端）
- yield {"type": "message", ...}         单轮流式消费完毕后的完整 assistant 消息
  （含拼装还原的 tool_calls，供上层执行工具后继续循环）
"""
import json
from typing import Any, AsyncGenerator, AsyncIterator

import httpx

from config import DEEPSEEK_API_KEY, DEEPSEEK_BASE_URL, DEEPSEEK_MODEL

MAX_ROUNDS = 3

SYSTEM_PROMPT = """你是"云观气象"城市气象大数据平台的 AI 助手。
数据范围：中国 371 个城市 2000-2020 年的降水/气温/日照/湿度/风速五个要素的历史统计。
规则：
1. 涉及具体数据必须调用工具查询，严禁编造任何数值。
2. factor 取值只能是 rain/temp/sun/hum/wind；年份限定 2000-2020；城市名用中文全称（如"福州市"）。
3. 回答用简体中文，先结论后数据，数值带单位，控制在 150 字以内。
4. 用户询问天气预报或 2020 年之后的情况时，说明平台仅提供 2000-2020 年历史统计。
5. 对话历史仅用于理解指代和上下文；只回答用户当前问题涉及的城市，不要重复查询或罗列历史消息中出现过的其他城市，除非用户明确要求。
"""

TOOLS = [
    {"type": "function", "function": {"name": "get_city_year_value", "description": "查询某城市某年某要素的数值及全国排名",
        "parameters": {"type": "object", "properties": {
            "city": {"type": "string", "description": "城市中文全称，如 福州市"},
            "factor": {"type": "string", "enum": ["rain", "temp", "sun", "hum", "wind"], "description": "要素"},
            "year": {"type": "integer", "minimum": 2000, "maximum": 2020, "description": "年份"}},
            "required": ["city", "factor", "year"]}}},
    {"type": "function", "function": {"name": "get_city_factor_summary", "description": "查询某城市某要素 2000-2020 年摘要：多年均值、最高最低值及出现年份",
        "parameters": {"type": "object", "properties": {
            "city": {"type": "string"}, "factor": {"type": "string", "enum": ["rain", "temp", "sun", "hum", "wind"]}},
            "required": ["city", "factor"]}}},
    {"type": "function", "function": {"name": "get_city_overview", "description": "查询某城市最新年份五要素一览",
        "parameters": {"type": "object", "properties": {"city": {"type": "string"}}, "required": ["city"]}}},
    {"type": "function", "function": {"name": "get_national_extreme", "description": "查询某年某要素全国排名前/后 N 的城市",
        "parameters": {"type": "object", "properties": {
            "factor": {"type": "string", "enum": ["rain", "temp", "sun", "hum", "wind"]},
            "year": {"type": "integer", "minimum": 2000, "maximum": 2020},
            "order": {"type": "string", "enum": ["top", "bottom"], "description": "top=最高，bottom=最低"},
            "limit": {"type": "integer", "minimum": 1, "maximum": 10, "description": "默认5"}},
            "required": ["factor", "year"]}}},
    {"type": "function", "function": {"name": "compare_cities", "description": "对比多个城市某年某要素数值与排名",
        "parameters": {"type": "object", "properties": {
            "cities": {"type": "array", "items": {"type": "string"}, "description": "城市全称数组，2-4个"},
            "factor": {"type": "string", "enum": ["rain", "temp", "sun", "hum", "wind"]},
            "year": {"type": "integer", "minimum": 2000, "maximum": 2020}},
            "required": ["cities", "factor", "year"]}}},
    {"type": "function", "function": {"name": "get_city_list", "description": "查询平台收录的城市名单，可按省份过滤。当用户提到的城市名可能不准确、询问某省/某地区有哪些城市、或需要确认城市写法时调用",
        "parameters": {"type": "object", "properties": {
            "province": {"type": "string", "description": "省份中文全称（如 海南省），不传返回全部省份分组"}},
            "required": []}}},
]


class LlmError(Exception):
    """DeepSeek 调用失败。"""


async def stream_chat(
    messages: list[dict[str, Any]], client: httpx.AsyncClient
) -> AsyncGenerator[dict[str, Any], None]:
    """流式调用 DeepSeek /chat/completions。

    content 增量以 {"type": "delta"} 产出；
    流结束时产出 {"type": "message", "content": ..., "tool_calls": [...]}。
    """
    body = {
        "model": DEEPSEEK_MODEL,
        "messages": messages,
        "tools": TOOLS,
        "temperature": 0.3,
        # v4 系列是推理模型：思考过程（reasoning_content）消耗同一条额，需给足
        "max_tokens": 2000,
        "stream": True,
    }
    headers = {"Authorization": f"Bearer {DEEPSEEK_API_KEY}"}

    content_parts: list[str] = []
    tool_calls: dict[int, dict[str, Any]] = {}

    async with client.stream(
        "POST",
        f"{DEEPSEEK_BASE_URL}/chat/completions",
        headers=headers,
        json=body,
        timeout=httpx.Timeout(120, connect=10),
    ) as resp:
        if resp.status_code != 200:
            raw = (await resp.aread()).decode("utf-8", errors="replace")
            raise LlmError(f"AI 服务调用失败({resp.status_code}): {raw[:200]}")

        async for line in resp.aiter_lines():
            if not line.startswith("data:"):
                continue
            data = line[5:].strip()
            if data == "[DONE]":
                break
            try:
                chunk = json.loads(data)
            except json.JSONDecodeError:
                continue
            delta = chunk.get("choices", [{}])[0].get("delta", {})
            if delta.get("content"):
                piece = delta["content"]
                content_parts.append(piece)
                yield {"type": "delta", "text": piece}
            for tc in delta.get("tool_calls") or []:
                idx = tc.get("index", 0)
                acc = tool_calls.setdefault(
                    idx, {"id": "", "type": "function", "function": {"name": "", "arguments": ""}}
                )
                if tc.get("id"):
                    acc["id"] = tc["id"]
                fn = tc.get("function") or {}
                if fn.get("name"):
                    acc["function"]["name"] = fn["name"]
                if fn.get("arguments"):
                    acc["function"]["arguments"] += fn["arguments"]

    msg: dict[str, Any] = {"role": "assistant"}
    msg["content"] = "".join(content_parts) if content_parts else None
    calls = [tool_calls[i] for i in sorted(tool_calls)]
    yield {
        "type": "message",
        "content": msg["content"],
        "tool_calls": calls,
    }
