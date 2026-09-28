import paramiko

client = paramiko.SSHClient()
client.set_missing_host_key_policy(paramiko.AutoAddPolicy())
client.connect('192.168.88.151', 22, 'root', 'hadoop', timeout=10)
cmd = "mysql -uroot -phadoop -e 'USE weather_db; SELECT id,username,password,nickname,role FROM sys_user;' 2>/dev/null"
_, stdout, _ = client.exec_command(cmd, timeout=30)
print(stdout.read().decode('utf-8', errors='replace'))
client.close()
