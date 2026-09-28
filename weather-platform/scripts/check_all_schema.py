import paramiko

client = paramiko.SSHClient()
client.set_missing_host_key_policy(paramiko.AutoAddPolicy())
client.connect('192.168.88.151', 22, 'root', 'hadoop', timeout=10)

def run(sql):
    cmd = f"mysql -uroot -phadoop -e \"{sql}\" 2>/dev/null"
    _, stdout, _ = client.exec_command(cmd, timeout=30)
    return stdout.read().decode('utf-8', errors='replace')

tables = [
    "ads_temp_ranking", "ads_temp_trend",
    "ads_sun_ranking", "ads_sun_trend",
    "ads_hum_ranking", "ads_hum_trend",
    "ads_wind_ranking", "ads_wind_trend",
]
for t in tables:
    print(f"=== {t} ===")
    print(run(f"USE weather_db; DESC {t};"))

client.close()
