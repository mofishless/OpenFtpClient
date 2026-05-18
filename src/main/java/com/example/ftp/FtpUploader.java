package com.example.ftp;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class FtpUploader {

    private static final int CONNECT_TIMEOUT_MS = 10_000;
    private static final int DATA_TIMEOUT_MS = 30_000;

    static int upload(String host, int port, String username, String password, String localFilePath) {
        File localFile = new File(localFilePath);
        if (!localFile.isFile()) {
            System.err.println("[ERROR] 文件不存在或不是普通文件: " + localFilePath);
            return 1;
        }

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

            String remoteName = localFile.getName();
            long fileSize = localFile.length();
            System.out.println("[INFO] 正在上传 " + remoteName + " (" + fileSize + " bytes) ...");

            boolean stored;
            try (InputStream input = new FileInputStream(localFile)) {
                stored = ftp.storeFile(remoteName, input);
            }

            if (!stored) {
                System.err.println("[ERROR] 上传失败，服务器响应码: " + ftp.getReplyCode());
                return 1;
            }
            System.out.println("[INFO] 上传成功: " + remoteName);
            return 0;

        } catch (IOException e) {
            System.err.println("[ERROR] 上传失败: " + e.getMessage());
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

    static void printUploadUsage() {
        System.err.println("上传模式用法: java -jar ftp-client.jar --upload <host> <port> <username> <password> <localFilePath>");
        System.err.println("示例: java -jar ftp-client.jar --upload 192.168.1.100 21 ftpuser mypassword ./test.txt");
    }
}
