package com.example.ftp;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;

import java.io.IOException;

public class FtpConnectTester {

    private static final int CONNECT_TIMEOUT_MS = 10_000;
    private static final int DATA_TIMEOUT_MS = 30_000;

    public static void main(String[] args) {
        if (args.length < 4) {
            System.err.println("用法: java -jar ftp-client.jar <host> <port> <username> <password>");
            System.err.println("示例: java -jar ftp-client.jar 192.168.1.100 21 ftpuser mypassword");
            System.exit(1);
        }

        String host = args[0];
        int port;
        try {
            port = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            System.err.println("[ERROR] 端口号无效: " + args[1]);
            System.exit(1);
            return;
        }
        String username = args[2];
        String password = args[3];

        int exitCode = testConnection(host, port, username, password);
        System.exit(exitCode);
    }

    static int testConnection(String host, int port, String username, String password) {
        FTPClient ftp = new FTPClient();
        ftp.setConnectTimeout(CONNECT_TIMEOUT_MS);
        ftp.setDataTimeout(DATA_TIMEOUT_MS);

        try {
            System.out.println("[INFO] 正在连接 " + host + ":" + port + " ...");
            ftp.connect(host, port);

            int replyCode = ftp.getReplyCode();
            if (!FTPReply.isPositiveCompletion(replyCode)) {
                System.err.println("[ERROR] 服务器拒绝连接，响应码: " + replyCode);
                return 1;
            }

            boolean loggedIn = ftp.login(username, password);
            if (!loggedIn) {
                System.err.println("[ERROR] 登录失败，请检查用户名和密码");
                return 1;
            }
            System.out.println("[INFO] 登录成功");

            ftp.enterLocalPassiveMode();

            FTPFile[] files = ftp.listFiles("/");
            if (files == null) {
                System.err.println("[ERROR] 获取目录列表失败");
                return 1;
            }

            System.out.println("[INFO] 根目录文件列表:");
            for (FTPFile file : files) {
                String type = file.isDirectory() ? "[DIR] " : "[FILE]";
                System.out.println("  " + type + " " + file.getName());
            }
            System.out.println("[INFO] 连接测试成功，共 " + files.length + " 个条目");
            return 0;

        } catch (IOException e) {
            System.err.println("[ERROR] 连接失败: " + e.getMessage());
            return 1;
        } finally {
            if (ftp.isConnected()) {
                try {
                    ftp.logout();
                } catch (IOException ignored) {
                }
                try {
                    ftp.disconnect();
                } catch (IOException ignored) {
                }
            }
        }
    }
}
