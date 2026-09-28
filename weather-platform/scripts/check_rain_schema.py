import paramiko

client = paramiko.SSHClient()
client.set_missing_host_key_policy(paramiko.AutoAddPolicy())
client.connect('192.168.88.151', 22, 'root', 'hadoop', timeout=10)

def run(sql):
    cmd = f"mysql -uroot -phadoop -e \"{sql}\" 2>/dev/null"
    _, stdout, _ = client.exec_command(cmd, timeout=30)
    return stdout.read().decode('utf-8', errors='replace')

print(run("USE weather_db; DESC ads_rain_ranking; DESC ads_rain_trend;"))
print(run("USE weather_db; SELECT * FROM ads_rain_ranking WHERE year=2020 ORDER BY rank_num;"))
print(run("USE weather_db; SELECT * FROM ads_rain_trend WHERE city='泉州市' ORDER BY year LIMIT 3;"))

client.close()
