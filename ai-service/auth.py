"""JWT 校验：与 Spring JwtUtil 共享同一 HS256 secret，PyJWT 直接解码。"""
import jwt
from fastapi import HTTPException, Request

from config import JWT_SECRET


async def require_user(request: Request) -> str:
    """FastAPI 依赖：校验 Bearer token，返回 username；失败抛 401。"""
    auth = request.headers.get("Authorization", "")
    if not auth.startswith("Bearer "):
        raise HTTPException(status_code=401, detail="未登录或登录已过期")
    token = auth[7:]
    try:
        payload = jwt.decode(token, JWT_SECRET, algorithms=["HS256"])
    except jwt.ExpiredSignatureError:
        raise HTTPException(status_code=401, detail="登录已过期")
    except jwt.InvalidTokenError:
        raise HTTPException(status_code=401, detail="无效 token")
    username = payload.get("username")
    if not username:
        raise HTTPException(status_code=401, detail="无效 token")
    return username
