"""连接 node1 执行 MySQL 查询，查看 weather_db 当前表状态"""
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
    print("=== weather_db 当前表清单 ===")
    print(run("USE weather_db; SHOW TABLES;"))
    print("=== ads_rain_ranking 行数（验证数据还在）===")
    print(run("USE weather_db; SELECT COUNT(*) AS cnt FROM ads_rain_ranking;"))
