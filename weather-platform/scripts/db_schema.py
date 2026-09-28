"""查看 ADS 层表结构，为后端实体类定义做准备"""
import paramiko

HOST, USER, PWD = "192.168.88.151", "root", "123456"

def run(sql: str):
    client = paramiko.SSHClient()
    client.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    client.connect(HOST, 22, USER, PWD, timeout=10)
    cmd = f'mysql -uroot -phadoop -e "{sql}" 2>/dev/null'
    _, stdout, _ = client.exec_command(cmd, timeout=30)
    out = stdout.read().decode("utf-8", errors="replace")
    client.close()
    return out

if __name__ == "__main__":
    tables = [
        "ads_rain_ranking", "ads_rain_trend",
        "ads_temp_ranking", "ads_temp_trend",
        "ads_sun_ranking", "ads_sun_trend",
        "ads_hum_ranking", "ads_hum_trend",
        "ads_wind_ranking", "ads_wind_trend",
    ]
    for t in tables:
        print(f"=== {t} ===")
        print(run(f"USE weather_db; DESC {t};"))
    print("=== 数据示例（2020年降水排名 / 泉州降水趋势前3行）===")
    print(run("USE weather_db; SELECT * FROM ads_rain_ranking WHERE year=2020;"))
    print(run("USE weather_db; SELECT * FROM ads_rain_trend WHERE city='泉州市' LIMIT 3;"))
