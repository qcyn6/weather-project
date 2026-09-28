import paramiko

client = paramiko.SSHClient()
client.set_missing_host_key_policy(paramiko.AutoAddPolicy())
client.connect('192.168.88.151', 22, 'root', 'hadoop', timeout=10)

def run(cmd):
    _, stdout, _ = client.exec_command(cmd, timeout=30)
    return stdout.read().decode('utf-8', errors='replace')

# 1. 取 tmpadmin 的 BCrypt 密文（SELECT 输出不受 shell 影响）
out = run("mysql -uroot -phadoop -N -e \"USE weather_db; SELECT password FROM sys_user WHERE username='tmpadmin';\" 2>/dev/null")
hashed = out.strip()
print(f"tmpadmin密文: {hashed[:20]}...")

if hashed.startswith('$2a$'):
    # 2. SQL 写入文件，SFTP 上传后执行，彻底避开 shell 的 $ 变量替换
    sql = (
        "USE weather_db;\n"
        f"UPDATE sys_user SET password='{hashed}' WHERE username='admin';\n"
        "DELETE FROM sys_user WHERE username='tmpadmin';\n"
    )
    sftp = client.open_sftp()
    with sftp.open('/tmp/update_admin.sql', 'w') as f:
        f.write(sql)
    sftp.close()
    print(run("mysql -uroot -phadoop < /tmp/update_admin.sql 2>&1"))
    run("rm -f /tmp/update_admin.sql")
    # 3. 验证
    print(run("mysql -uroot -phadoop -e \"USE weather_db; SELECT id,username,LEFT(password,7) AS pwd_prefix,role FROM sys_user;\" 2>/dev/null"))
else:
    print("未找到tmpadmin密文，请先注册tmpadmin")

client.close()
