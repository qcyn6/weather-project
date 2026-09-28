package com.weather.service;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/**
 * 通过 SSH 触发 node1 上的聚类重跑作业（spark-shell + KMeans）。
 * 单节点实验环境用密码直连；生产应换成密钥与堡垒机。
 */
@Slf4j
@Service
public class ReclusterService {

    private static final String COMMAND =
            "export PATH=/export/server/spark-3.1.2-bin-hadoop3.2/bin:/export/server/hadoop-3.3.0/bin:$PATH; "
            + "mysql -uroot -phadoop -e 'TRUNCATE weather_db.ads_city_cluster; TRUNCATE weather_db.ads_cluster_profile;' && "
            + "spark-shell --master local[*] --driver-memory 1g "
            + "--jars /export/server/apache-hive-3.1.2-bin/lib/mysql-connector-java-5.1.32.jar "
            + "-i /tmp/cluster_etl.scala";

    @Value("${manage.ssh.host}")
    private String host;
    @Value("${manage.ssh.port}")
    private int port;
    @Value("${manage.ssh.user}")
    private String user;
    @Value("${manage.ssh.password}")
    private String password;

    /** 执行重跑，返回作业日志尾部（调用方应限制触发频率） */
    public String recluster() {
        Session session = null;
        ChannelExec channel = null;
        try {
            JSch jsch = new JSch();
            session = jsch.getSession(user, host, port);
            session.setPassword(password);
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect(10_000);

            channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand(COMMAND);
            channel.setInputStream(null);
            InputStream out = channel.getInputStream();
            channel.connect();

            StringBuilder sb = new StringBuilder();
            byte[] buf = new byte[4096];
            long deadline = System.nanoTime() + TimeUnit.MINUTES.toNanos(6);
            while (System.nanoTime() < deadline) {
                while (out.available() > 0) {
                    int n = out.read(buf);
                    if (n < 0) {
                        break;
                    }
                    sb.append(new String(buf, 0, n, StandardCharsets.UTF_8));
                }
                if (channel.isClosed()) {
                    break;
                }
                TimeUnit.MILLISECONDS.sleep(500);
            }
            String log = sb.toString();
            String marker = log.contains("CLUSTER_DONE") ? "作业成功" : "作业未正常结束";
            return marker + " | " + tail(log, 600);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("SSH 执行被中断", e);
        } catch (Exception e) {
            log.error("重跑聚类失败", e);
            throw new IllegalStateException("SSH 触发失败: " + e.getMessage(), e);
        } finally {
            if (channel != null) {
                channel.disconnect();
            }
            if (session != null) {
                session.disconnect();
            }
        }
    }

    private String tail(String s, int max) {
        String clean = s.replaceAll("\r", "").trim();
        return clean.length() <= max ? clean : clean.substring(clean.length() - max);
    }
}
