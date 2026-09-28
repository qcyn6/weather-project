<template>
  <transition name="drawer">
    <div v-if="open" class="ai-drawer">
      <div class="ai-head">
        <span class="material-symbols-outlined" style="color: var(--ink-secondary)">smart_toy</span>
        <span class="t-title">云观 AI 助手</span>
        <span class="t-label ai-sub">DeepSeek · 数据实时查询</span>
        <button class="ai-close" @click="emit('close')">
          <span class="material-symbols-outlined">close</span>
        </button>
      </div>

      <div ref="msgsRef" class="ai-msgs">
        <div v-if="!messages.length" class="ai-welcome">
          <p class="welcome-text">你好，我是云观 AI 助手，可以帮你查询 371 个城市 2000-2020 年的五要素气候数据。</p>
          <div class="quick-list">
            <button
              v-for="q in QUICK_QUESTIONS"
              :key="q"
              class="btn-ghost quick-q"
              @click="send(q)"
            >
              {{ q }}
            </button>
          </div>
        </div>

        <template v-for="(m, i) in messages" :key="i">
          <div class="bubble-row" :class="m.role">
            <div v-if="m.trace" class="trace-chip t-label">🔍 {{ m.trace }}</div>
            <div class="bubble" :class="m.role" v-html="renderMd(m.content)"></div>
            <span v-if="m.streaming && m.content" class="cursor">▌</span>
          </div>
        </template>
      </div>

      <div class="ai-input">
        <input
          v-model="draft"
          type="text"
          :placeholder="loading ? '生成中，可点击停止…' : '例如：福州 2018 年降水怎么样？'"
          maxlength="200"
          :disabled="loading"
          @keyup.enter="send()"
        />
        <button
          v-if="loading"
          class="btn stop-btn"
          @click="stop()"
        >
          <span class="material-symbols-outlined">stop_circle</span>
          停止
        </button>
        <button v-else class="btn btn-primary send-btn" :disabled="!draft.trim()" @click="send()">
          发送
        </button>
      </div>
    </div>
  </transition>
</template>

<script setup lang="ts">
import { ref, reactive, nextTick, watch } from 'vue'
import { chatAiStream, type AiHistoryItem } from '@/api/ai'

const props = defineProps<{ open: boolean }>()
const emit = defineEmits<{ (e: 'close'): void }>()

const QUICK_QUESTIONS = [
  '全国降水最多的城市是哪个？',
  '对比福州和三亚的气温',
  '福州 2018 年降水怎么样？',
  '吐鲁番的五要素一览'
]

interface ChatMessage {
  role: 'user' | 'assistant'
  content: string
  /** 本次回答经过的工具查询（展示在气泡上方） */
  trace?: string
  /** 回答是否仍在生成中 */
  streaming?: boolean
}

const messages = ref<ChatMessage[]>([])
const draft = ref('')
const loading = ref(false)
const msgsRef = ref<HTMLElement>()
let abortController: AbortController | null = null

watch(
  () => props.open,
  open => {
    if (open) {
      nextTick(() => scrollBottom(true))
    }
  }
)

/** 仅当用户本就在底部附近时才跟随滚动，避免打断上翻阅读 */
function isNearBottom(): boolean {
  const el = msgsRef.value
  if (!el) return true
  return el.scrollHeight - el.scrollTop - el.clientHeight < 80
}

function scrollBottom(force = false) {
  const el = msgsRef.value
  if (el && (force || isNearBottom())) {
    el.scrollTop = el.scrollHeight
  }
}

// ---------- 极简 Markdown：加粗 / 无序列表 / 换行（先转义防 XSS） ----------
function escapeHtml(s: string): string {
  return s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
}

function renderMd(text: string): string {
  const out: string[] = []
  let inList = false
  for (const line of escapeHtml(text).split('\n')) {
    const l = line.replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
    if (/^\s*[-*]\s+/.test(l)) {
      if (!inList) {
        out.push('<ul>')
        inList = true
      }
      out.push(`<li>${l.replace(/^\s*[-*]\s+/, '')}</li>`)
    } else {
      if (inList) {
        out.push('</ul>')
        inList = false
      }
      out.push(l.trim() === '' ? '<br>' : `<div>${l}</div>`)
    }
  }
  if (inList) out.push('</ul>')
  return out.join('')
}

async function send(preset?: string) {
  const q = (preset ?? draft.value).trim()
  if (!q || loading.value) return
  draft.value = ''
  messages.value.push({ role: 'user', content: q })
  await nextTick(() => scrollBottom(true))

  loading.value = true
  abortController = new AbortController()
  const assistantMsg = reactive<ChatMessage>({ role: 'assistant', content: '', streaming: true })
  messages.value.push(assistantMsg)

  const history: AiHistoryItem[] = messages.value
    .slice(0, -2)
    .filter(m => (m.role === 'user' || m.role === 'assistant') && m.content)
    .slice(-6)
    .map(m => ({ role: m.role, content: m.content }))

  try {
    await chatAiStream(q, history, {
      onTrace: (text: string) => {
        assistantMsg.trace = assistantMsg.trace ? `${assistantMsg.trace}；${text}` : text
        nextTick(() => scrollBottom())
      },
      onDelta: (text: string) => {
        assistantMsg.content += text
        nextTick(() => scrollBottom())
      },
      onDone: () => {},
      onError: (message: string) => {
        if (!assistantMsg.content) {
          assistantMsg.content = `抱歉，${message}。`
        }
      },
      onAbort: () => {}
    }, abortController.signal)
    if (!assistantMsg.content) {
      assistantMsg.content = '（未返回内容，请重试）'
    }
  } catch (e) {
    if (abortController?.signal.aborted) {
      assistantMsg.content += assistantMsg.content ? '' : '（已停止生成）'
    } else {
      assistantMsg.content = '抱歉，AI 连接中断，请重试。'
    }
  } finally {
    assistantMsg.streaming = false
    abortController = null
    loading.value = false
    await nextTick(scrollBottom)
  }
}

function stop() {
  abortController?.abort()
}
</script>

<style scoped lang="scss">
.ai-drawer {
  position: fixed;
  top: 0;
  right: 0;
  bottom: 0;
  width: 400px;
  max-width: 100vw;
  background: var(--ink-surface);
  border-left: 1px solid var(--ink-outline-variant);
  box-shadow: -8px 0 30px rgba(0, 0, 0, 0.15);
  display: flex;
  flex-direction: column;
  z-index: 200;
}

.drawer-enter-active,
.drawer-leave-active {
  transition: transform 0.28s ease, opacity 0.28s ease;
}
.drawer-enter-from,
.drawer-leave-to {
  transform: translateX(100%);
  opacity: 0;
}

.ai-head {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 16px 18px;
  border-bottom: 1px solid var(--ink-outline-variant);
  flex-shrink: 0;

  .ai-sub {
    flex: 1;
    color: var(--ink-outline);
  }

  .ai-close {
    width: 32px;
    height: 32px;
    border: none;
    border-radius: 50%;
    background: transparent;
    color: var(--ink-text-variant);
    cursor: pointer;
    display: flex;
    align-items: center;
    justify-content: center;

    &:hover {
      background: var(--ink-bg-high);
    }

    .material-symbols-outlined {
      font-size: 20px;
    }
  }
}

.ai-msgs {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.ai-welcome {
  .welcome-text {
    margin: 0 0 14px;
    font-size: 14px;
    line-height: 1.7;
    color: var(--ink-text-variant);
  }

  .quick-list {
    display: flex;
    flex-direction: column;
    gap: 8px;

    .quick-q {
      text-align: left;
      justify-content: flex-start;
    }
  }
}

.bubble-row {
  display: flex;
  flex-direction: column;
  align-items: flex-end;

  &.assistant {
    align-items: flex-start;
  }

  .trace-chip {
    margin-bottom: 4px;
    padding: 3px 10px;
    background: rgba(31, 68, 97, 0.08);
    border: 1px solid rgba(31, 68, 97, 0.2);
    border-radius: 999px;
    color: var(--ink-tertiary-container);
  }

  .bubble {
    max-width: 88%;
    padding: 10px 14px;
    border-radius: 10px;
    font-size: 14px;
    line-height: 1.7;
    white-space: pre-wrap;
    word-break: break-word;

    &.user {
      background: var(--ink-primary-container);
      color: #fff;
      border-bottom-right-radius: 3px;
    }

    &.assistant {
      background: var(--ink-bg-low);
      border: 1px solid var(--ink-outline-variant);
      color: var(--ink-text);
      border-bottom-left-radius: 3px;
    }
  }
}

.typing .dots i {
  animation: blink 1.2s infinite;
  font-style: normal;

  &:nth-child(2) { animation-delay: 0.2s; }
  &:nth-child(3) { animation-delay: 0.4s; }
}

@keyframes blink {
  0%, 100% { opacity: 0.2; }
  50% { opacity: 1; }
}

.cursor {
  color: var(--ink-secondary);
  animation: blink 0.8s infinite;
  font-size: 14px;
  margin-top: 2px;
}

.stop-btn {
  background: var(--ink-error);
  color: #fff;

  .material-symbols-outlined {
    font-size: 18px;
  }

  &:hover:not(:disabled) {
    background: #991515;
  }
}

.bubble :deep(ul) {
  margin: 4px 0;
  padding-left: 18px;
}

.bubble :deep(strong) {
  color: inherit;
}

.ai-input {
  flex-shrink: 0;
  display: flex;
  gap: 10px;
  padding: 14px 16px;
  border-top: 1px solid var(--ink-outline-variant);

  input {
    flex: 1;
    background: var(--ink-bg-low);
    border: 1px solid var(--ink-outline-variant);
    border-radius: 8px;
    padding: 10px 14px;
    font-size: 14px;
    color: var(--ink-text);
    outline: none;
    transition: border-color 0.25s;

    &:focus {
      border-color: var(--ink-primary-container);
    }
  }

  .send-btn {
    padding: 10px 18px;
  }
}
</style>
