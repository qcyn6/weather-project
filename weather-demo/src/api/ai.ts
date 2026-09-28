export interface AiHistoryItem {
  role: 'user' | 'assistant'
  content: string
}

export interface StreamHandlers {
  onTrace: (text: string) => void
  onDelta: (text: string) => void
  onDone: () => void
  onError: (message: string) => void
  onAbort?: () => void
}

/**
 * 流式 AI 聊天：POST SSE，事件序列 trace* -> delta* -> done / error。
 * token 手动从 localStorage 注入（fetch 流不走 axios 拦截器）。
 */
export async function chatAiStream(
  question: string,
  history: AiHistoryItem[],
  handlers: StreamHandlers,
  signal?: AbortSignal
): Promise<void> {
  let resp: Response
  try {
    resp = await fetch('/api/ai/chat/stream', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${localStorage.getItem('weather_token') || ''}`
      },
      body: JSON.stringify({ question, history }),
      signal
    })
  } catch (e) {
    if ((e as Error).name === 'AbortError') {
      handlers.onAbort?.()
    } else {
      handlers.onError('网络异常，无法连接 AI 服务')
    }
    return
  }
  if (!resp.ok || !resp.body) {
    handlers.onError(`AI 服务请求失败(${resp.status})`)
    return
  }

  const reader = resp.body.getReader()
  const decoder = new TextDecoder('utf-8')
  let buffer = ''

  try {
    for (;;) {
      const { done, value } = await reader.read()
      if (done) break
      buffer += decoder.decode(value, { stream: true })

      // SSE 帧以空行分隔：event: xxx\ndata: yyy\n\n
      let sep: number
      while ((sep = buffer.indexOf('\n\n')) >= 0) {
        const frame = buffer.slice(0, sep)
        buffer = buffer.slice(sep + 2)
        let event = 'message'
        let data = ''
        for (const line of frame.split('\n')) {
          if (line.startsWith('event:')) {
            event = line.slice(6).trim()
          } else if (line.startsWith('data:')) {
            data += line.slice(5).trim()
          }
        }
        if (!data) continue
        let payload: { text?: string; message?: string } = {}
        try {
          payload = JSON.parse(data)
        } catch {
          continue
        }
        if (event === 'trace' && payload.text) {
          handlers.onTrace(payload.text)
        } else if (event === 'delta' && payload.text) {
          handlers.onDelta(payload.text)
        } else if (event === 'done') {
          handlers.onDone()
        } else if (event === 'error') {
          handlers.onError(payload.message || 'AI 服务暂不可用')
        }
      }
    }
  } catch (e) {
    if ((e as Error).name !== 'AbortError') {
      handlers.onError('AI 连接中断')
    } else {
      handlers.onAbort?.()
    }
  }
}
