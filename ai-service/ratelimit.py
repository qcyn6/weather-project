"""滑动窗口限流：每用户每 60 秒最多 10 次（与 Java 版 AiController 同语义）。"""
import time
from collections import defaultdict, deque
from threading import Lock

from config import RATE_LIMIT_MAX, RATE_LIMIT_WINDOW


class RateLimitError(Exception):
    pass


_hits: dict[str, deque] = defaultdict(deque)
_lock = Lock()


def check(username: str) -> None:
    now = time.time()
    with _lock:
        dq = _hits[username]
        while dq and now - dq[0] > RATE_LIMIT_WINDOW:
            dq.popleft()
        if len(dq) >= RATE_LIMIT_MAX:
            raise RateLimitError("提问太频繁，请稍后再试")
        dq.append(now)
