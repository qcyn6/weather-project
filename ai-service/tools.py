"""六个只读查询工具：从 Java AiService 1:1 平移（PyMySQL 参数化查询 + 白名单）。"""
import json
from typing import Any

import pymysql

from dbutils.pooled_db import PooledDB

from config import DB_HOST, DB_NAME, DB_PASSWORD, DB_PORT, DB_USER

# factor -> (排名表, 趋势表, 主指标列, 单位, 中文名)
FACTORS = {
    "rain": ("ads_rain_ranking", "ads_rain_trend", "total_rain", "mm", "降水"),
    "temp": ("ads_temp_ranking", "ads_temp_trend", "avg_temp", "°C", "气温"),
    "sun": ("ads_sun_ranking", "ads_sun_trend", "total_sun", "h", "日照"),
    "hum": ("ads_hum_ranking", "ads_hum_trend", "avg_humidity", "%", "湿度"),
    "wind": ("ads_wind_ranking", "ads_wind_trend", "avg_wind", "m/s", "风速"),
}

_pool = PooledDB(
    creator=pymysql,
    mincached=1,
    maxcached=4,
    host=DB_HOST,
    port=DB_PORT,
    user=DB_USER,
    password=DB_PASSWORD,
    database=DB_NAME,
    charset="utf8mb4",
    cursorclass=pymysql.cursors.DictCursor,
    autocommit=True,
)


def _query_all(sql: str, params: tuple = ()) -> list[dict[str, Any]]:
    with _pool.connection() as conn:
        with conn.cursor() as cur:
            cur.execute(sql, params)
            return cur.fetchall()


def _query_one(sql: str, params: tuple = ()) -> Any:
    rows = _query_all(sql, params)
    return rows[0] if rows else None


def _execute(sql: str, params: tuple = ()) -> None:
    with _pool.connection() as conn:
        with conn.cursor() as cur:
            cur.execute(sql, params)


def normalize_city(city: str) -> str:
    """兜底：城市名缺"市"后缀时补全再查（盟/县/旗/林区等不改）。"""
    trimmed = (city or "").strip()
    if not trimmed:
        return trimmed
    if trimmed.endswith(("市", "州", "盟", "县", "旗", "林区")):
        return trimmed
    hit = _query_one("SELECT city FROM ads_rain_ranking WHERE city = %s", (trimmed + "市",))
    return hit["city"] if hit else trimmed


def _factor(factor: str) -> tuple:
    f = FACTORS.get(factor)
    if not f:
        raise ValueError("不支持的气象要素")
    return f


# ---------- 六个工具 ----------

def get_city_year_value(city: str, factor: str, year: int) -> dict:
    f = _factor(factor)
    row = _query_one(
        f"SELECT rank_num, {f[2]} AS value FROM {f[0]} WHERE city = %s AND year = %s",
        (city, year),
    )
    if not row:
        city = normalize_city(city)
        row = _query_one(
            f"SELECT rank_num, {f[2]} AS value FROM {f[0]} WHERE city = %s AND year = %s",
            (city, year),
        )
    avg_row = _query_one(f"SELECT AVG({f[2]}) AS avg FROM {f[0]} WHERE year = %s", (year,))
    node: dict[str, Any] = {"city": city, "factor": f[4], "year": year}
    if row and row["value"] is not None:
        node.update({
            "value": row["value"], "unit": f[3], "nationalRank": row["rank_num"],
            "nationalAvg": round(float(avg_row["avg"]), 2) if avg_row["avg"] is not None else None,
        })
    else:
        node["error"] = "该城市此年份无数据"
    return node


def get_city_factor_summary(city: str, factor: str) -> dict:
    f = _factor(factor)
    rows = _query_all(
        f"SELECT year, {f[2]} AS value FROM {f[1]} WHERE city = %s ORDER BY year", (city,)
    )
    if not rows:
        city = normalize_city(city)
        rows = _query_all(
            f"SELECT year, {f[2]} AS value FROM {f[1]} WHERE city = %s ORDER BY year", (city,)
        )
    node: dict[str, Any] = {"city": city, "factor": f[4], "unit": f[3]}
    if not rows:
        node["error"] = "未找到该城市数据"
        return node
    values = [float(r["value"]) for r in rows if r["value"] is not None]
    max_row = max(rows, key=lambda r: float(r["value"]))
    min_row = min(rows, key=lambda r: float(r["value"]))
    node.update({
        "years": len(rows),
        "avg": round(sum(values) / len(values), 2),
        "max": float(max_row["value"]), "maxYear": max_row["year"],
        "min": float(min_row["value"]), "minYear": min_row["year"],
        "firstYearValue": float(rows[0]["value"]),
        "lastYear": rows[-1]["year"], "lastYearValue": float(rows[-1]["value"]),
    })
    return node


def get_city_overview(city: str) -> dict:
    used = city
    latest_row = _query_one("SELECT MAX(year) AS y FROM ads_rain_ranking")
    latest = latest_row["y"] if latest_row else None
    node: dict[str, Any] = {"city": used, "year": latest}
    factors = node.setdefault("factors", {})
    for key, f in FACTORS.items():
        row = _query_one(
            f"SELECT rank_num, {f[2]} AS value FROM {f[0]} WHERE city = %s AND year = %s",
            (used, latest),
        )
        if not row and not used.endswith("市"):
            used = used + "市"
            row = _query_one(
                f"SELECT rank_num, {f[2]} AS value FROM {f[0]} WHERE city = %s AND year = %s",
                (used, latest),
            )
        if row and row["value"] is not None:
            factors[key] = {"cnName": f[4], "value": row["value"], "unit": f[3], "rank": row["rank_num"]}
    node["resolvedCity"] = used
    return node


def get_national_extreme(factor: str, year: int, order: str = "top", limit: int = 5) -> dict:
    f = _factor(factor)
    n = max(1, min(limit, 10))
    direction = "ASC" if order == "bottom" else "DESC"
    rows = _query_all(
        f"SELECT city, province, {f[2]} AS value FROM {f[0]} WHERE year = %s "
        f"ORDER BY {f[2]} {direction} LIMIT {n}",
        (year,),
    )
    cities = [
        {"rank": i + 1, "city": r["city"], "province": r["province"], "value": float(r["value"])}
        for i, r in enumerate(rows)
    ]
    return {"factor": f[4], "year": year, "order": order, "unit": f[3], "cities": cities}


def compare_cities(cities: list[str], factor: str, year: int) -> dict:
    f = _factor(factor)
    if not cities:
        return {"error": "请提供城市列表"}
    placeholders = ", ".join(["%s"] * len(cities))
    rows = _query_all(
        f"SELECT city, province, rank_num, {f[2]} AS value FROM {f[0]} "
        f"WHERE year = %s AND city IN ({placeholders}) ORDER BY {f[2]} DESC",
        (year, *cities),
    )
    return {
        "factor": f[4], "year": year, "unit": f[3],
        "cities": [
            {"city": r["city"], "province": r["province"],
             "value": float(r["value"]), "rank": r["rank_num"]}
            for r in rows
        ],
    }


def get_city_list(province: str | None = None) -> dict:
    if province:
        rows = _query_all(
            "SELECT DISTINCT province, city FROM ads_rain_ranking WHERE province = %s "
            "ORDER BY province, city",
            (province,),
        )
    else:
        rows = _query_all(
            "SELECT DISTINCT province, city FROM ads_rain_ranking ORDER BY province, city"
        )
    by_province: dict[str, list[str]] = {}
    for r in rows:
        by_province.setdefault(r["province"], []).append(r["city"])
    return {"total": len(rows), "byProvince": by_province}


# ---------- 分发与审计 ----------

def execute_tool(name: str, args: dict) -> dict:
    try:
        if name == "get_city_year_value":
            return get_city_year_value(args.get("city", ""), args.get("factor", ""), int(args.get("year", 0)))
        if name == "get_city_factor_summary":
            return get_city_factor_summary(args.get("city", ""), args.get("factor", ""))
        if name == "get_city_overview":
            return get_city_overview(args.get("city", ""))
        if name == "get_national_extreme":
            return get_national_extreme(
                args.get("factor", ""), int(args.get("year", 0)),
                args.get("order", "top"), int(args.get("limit", 5)))
        if name == "compare_cities":
            return compare_cities(args.get("cities", []), args.get("factor", ""), int(args.get("year", 0)))
        if name == "get_city_list":
            return get_city_list(args.get("province") or None)
        return {"error": "未知工具"}
    except (ValueError, TypeError):
        raise
    except Exception as exc:  # 查询失败交回 LLM 组织话术
        return {"error": f"查询失败：{exc}"}


def describe_tool(name: str, args: dict) -> str:
    factor_cn = FACTORS.get(args.get("factor", ""), ("", "", "", "", args.get("factor", "")))[4]
    if name == "get_city_year_value":
        return f"{args.get('city', '')} {args.get('year', '')} 年{factor_cn}"
    if name == "get_city_factor_summary":
        return f"{args.get('city', '')}{factor_cn}历史摘要"
    if name == "get_city_overview":
        return f"{args.get('city', '')} 五要素一览"
    if name == "get_national_extreme":
        order = "最低" if args.get("order") == "bottom" else "最高"
        return f"全国 {args.get('year', '')} 年{factor_cn}{order}榜"
    if name == "compare_cities":
        return f"多城{factor_cn}对比"
    if name == "get_city_list":
        prov = args.get("province") or ""
        return f"{prov}城市名单" if prov else "平台城市名单"
    return name


def log_operation(username: str, action: str, detail: str) -> None:
    try:
        _execute(
            "INSERT INTO sys_operation_log (username, action, detail) VALUES (%s, %s, %s)",
            (username, action, detail),
        )
    except Exception:
        pass  # 审计失败不影响主流程


def db_up() -> bool:
    try:
        _query_one("SELECT 1")
        return True
    except Exception:
        return False
