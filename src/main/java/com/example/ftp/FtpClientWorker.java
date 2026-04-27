package com.example.ftp;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

public class FtpClientWorker implements Runnable {

    private static final int CONNECT_TIMEOUT_MS = 10_000;
    private static final int DATA_TIMEOUT_MS = 30_000;
    private static final int KEEPALIVE_INTERVAL_MS = 30_000;

    private final String clientId;
    private final ConnectionConfig config;
    private final AtomicBoolean running = new AtomicBoolean(true);
    private volatile FTPClient ftp;
    private volatile Thread workerThread;

    FtpClientWorker(String clientId, ConnectionConfig config) {
        this.clientId = clientId;
        this.config = config;
    }

    @Override
    public void run() {
        ftp = new FTPClient();
        workerThread = Thread.currentThread();
        ftp.setConnectTimeout(CONNECT_TIMEOUT_MS);
        ftp.setDataTimeout(DATA_TIMEOUT_MS);

        try {
            log("正在连接 " + config.host + ":" + config.port + " ...");
            ftp.connect(config.host, config.port);

            if (!FTPReply.isPositiveCompletion(ftp.getReplyCode())) {
                err("服务器拒绝连接，响应码: " + ftp.getReplyCode());
                return;
            }

            if (!ftp.login(config.username, config.password)) {
                err("登录失败，请检查用户名和密码");
                return;
            }
            log("登录成功");

            ftp.enterLocalPassiveMode();
            log("连接模式: 被动模式 (PASV)");

            FTPFile[] files = ftp.listFiles("/");
            if (files == null) {
                err("获取目录列表失败");
                return;
            }

            log("被动模式数据端口: " + ftp.getPassivePort()
                    + " (服务器地址: " + ftp.getPassiveHost() + ")");
            log("根目录文件列表:");
            for (FTPFile file : files) {
                String type = file.isDirectory() ? "[DIR] " : "[FILE]";
                System.out.println("  [" + clientId + "] " + type + " " + file.getName());
            }
            log("目录列表完成，共 " + files.length + " 个条目，连接保持中（每 30s 发送 NOOP）...");

            while (running.get()) {
                try {
                    Thread.sleep(KEEPALIVE_INTERVAL_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
                if (!running.get()) break;
                boolean ok = ftp.sendNoOp();
                if (ok) {
                    log("NOOP 心跳 OK");
                } else {
                    err("NOOP 失败，连接可能已断开");
                    break;
                }
            }

        } catch (IOException e) {
            err("连接失败: " + e.getMessage());
        } finally {
            disconnect();
        }
    }

    void shutdown() {
        running.set(false);
        Thread t = workerThread;
        if (t != null) {
            t.interrupt();
        }
        disconnect();
    }

    private void disconnect() {
        FTPClient f = ftp;
        if (f != null && f.isConnected()) {
            try {
                f.logout();
            } catch (IOException ignored) {
            }
            try {
                f.disconnect();
                log("已断开连接");
            } catch (IOException ignored) {
            }
        }
    }

    private void log(String msg) {
        System.out.println("[INFO][" + clientId + "] " + msg);
    }

    private void err(String msg) {
        System.err.println("[ERROR][" + clientId + "] " + msg);
    }
}
