"""环境变量加载。默认值与 Spring 版 application.yml 保持一致，迁移零配置。"""
import os

from dotenv import load_dotenv

load_dotenv()

# DeepSeek（OpenAI 兼容协议）
DEEPSEEK_BASE_URL = os.getenv("DEEPSEEK_BASE_URL", "https://api.deepseek.com")
DEEPSEEK_API_KEY = os.getenv("DEEPSEEK_API_KEY", os.getenv("OPENAI_API_KEY", ""))
DEEPSEEK_MODEL = os.getenv("DEEPSEEK_MODEL", "deepseek-v4-flash")

# JWT：与 Spring JwtUtil 共享同一 secret（HS256）
JWT_SECRET = os.getenv("JWT_SECRET", "weather-platform-jwt-secret-2026-0827")

# MySQL（只读查询 weather_db）
DB_HOST = os.getenv("DB_HOST", "192.168.88.151")
DB_PORT = int(os.getenv("DB_PORT", "3306"))
DB_USER = os.getenv("DB_USER", os.getenv("DB_USERNAME", "root"))
DB_PASSWORD = os.getenv("DB_PASSWORD", "hadoop")
DB_NAME = os.getenv("DB_NAME", "weather_db")

# 限流
RATE_LIMIT_MAX = int(os.getenv("AI_RATE_LIMIT_MAX", "10"))
RATE_LIMIT_WINDOW = float(os.getenv("AI_RATE_LIMIT_WINDOW", "60"))
