"""任务0-2：建 sys_user 表 + 预置 admin 占位账号"""
import paramiko

HOST, USER, PWD = "192.168.88.151", "root", "123456"

CREATE_SQL = """CREATE TABLE IF NOT EXISTS sys_user (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  username    VARCHAR(50)  NOT NULL UNIQUE COMMENT '登录名',
  password    VARCHAR(100) NOT NULL COMMENT 'BCrypt加密密码',
  nickname    VARCHAR(50)  COMMENT '昵称',
  role        VARCHAR(20)  DEFAULT 'user' COMMENT 'user/admin',
  create_time DATETIME     DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='系统用户'"""

def run(sql: str, label: str):
    client = paramiko.SSHClient()
    client.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    client.connect(HOST, 22, USER, PWD, timeout=10)
    # 用 heredoc 传 SQL，避免引号转义问题
    cmd = f"mysql -uroot -phadoop weather_db <<'EOSQL'\n{sql}\nEOSQL"
    _, stdout, stderr = client.exec_command(cmd, timeout=30)
    out = stdout.read().decode("utf-8", errors="replace")
    err = stderr.read().decode("utf-8", errors="replace")
    client.close()
    # mysql 5.x 密码警告属正常噪声
    err_clean = "\n".join(l for l in err.splitlines() if "Using a password" not in l)
    print(f"--- {label} ---")
    if out.strip():
        print(out)
    if err_clean.strip():
        print("STDERR:", err_clean)
    return out

if __name__ == "__main__":
    # 1. 建表（幂等）
    run(CREATE_SQL, "建 sys_user 表")

    # 2. 预置 admin 占位（密码先放明文占位，后端任务1-2时用BCrypt更新）
    run("INSERT INTO sys_user (username, password, nickname, role) "
        "VALUES ('admin', 'PLACEHOLDER', '管理员', 'admin') "
        "ON DUPLICATE KEY UPDATE nickname='管理员';",
        "预置 admin 账号")

    # 3. 验证
    print("--- 最终验证 ---")
    run("SHOW TABLES;", "表清单")
    run("SELECT id, username, nickname, role, create_time FROM sys_user;", "sys_user 内容")
