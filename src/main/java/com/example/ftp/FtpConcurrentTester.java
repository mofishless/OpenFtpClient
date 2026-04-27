package com.example.ftp;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class FtpConcurrentTester {

    static void run(String[] args) {
        List<ConnectionConfig> configs = parseArgs(args);
        if (configs.isEmpty()) {
            System.err.println("[ERROR] 未找到任何连接配置");
            System.exit(1);
        }

        System.out.println("[INFO] 启动 " + configs.size() + " 个并发 FTP 连接...");

        List<FtpClientWorker> workers = new ArrayList<>();
        for (int i = 0; i < configs.size(); i++) {
            workers.add(new FtpClientWorker("Client-" + (i + 1), configs.get(i)));
        }

        ExecutorService executor = Executors.newFixedThreadPool(workers.size());

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n[INFO] 收到退出信号，正在断开所有连接...");
            for (FtpClientWorker w : workers) {
                w.shutdown();
            }
            executor.shutdownNow();
            try {
                executor.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException ignored) {
            }
            System.out.println("[INFO] 所有连接已断开");
        }));

        for (FtpClientWorker w : workers) {
            executor.submit(w);
        }
        executor.shutdown();

        try {
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
        } catch (InterruptedException ignored) {
        }
    }

    private static List<ConnectionConfig> parseArgs(String[] args) {
        // args[0] is already consumed ("--concurrent"), so args here starts from index 1 onward
        // Passed in as the slice after "--concurrent"
        List<ConnectionConfig> configs = new ArrayList<>();

        if (args.length == 1) {
            // Single argument: treat as config file path
            configs.addAll(loadFromFile(args[0]));
        } else if (args.length >= 4 && (args.length - 2) % 2 == 0) {
            // host port user1 pass1 [user2 pass2 ...]
            String host = args[0];
            int port = parsePort(host, args[1]);
            if (port < 0) return configs;
            for (int i = 2; i + 1 < args.length; i += 2) {
                configs.add(new ConnectionConfig(host, port, args[i], args[i + 1]));
            }
        } else if (args.length >= 4 && args.length % 4 == 0) {
            // host port user pass [host port user pass ...]
            for (int i = 0; i + 3 < args.length; i += 4) {
                int port = parsePort(args[i], args[i + 1]);
                if (port < 0) return new ArrayList<>();
                configs.add(new ConnectionConfig(args[i], port, args[i + 2], args[i + 3]));
            }
        } else {
            System.err.println("[ERROR] 并发模式参数格式错误");
            printConcurrentUsage();
        }
        return configs;
    }

    private static List<ConnectionConfig> loadFromFile(String path) {
        List<ConnectionConfig> configs = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            String line;
            int lineNum = 0;
            while ((line = br.readLine()) != null) {
                lineNum++;
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                String[] parts = line.split("\\s+");
                if (parts.length != 4) {
                    System.err.println("[WARN] 第 " + lineNum + " 行格式错误，跳过: " + line);
                    continue;
                }
                int port = parsePort(parts[0], parts[1]);
                if (port < 0) continue;
                configs.add(new ConnectionConfig(parts[0], port, parts[2], parts[3]));
            }
        } catch (IOException e) {
            System.err.println("[ERROR] 读取配置文件失败: " + e.getMessage());
        }
        return configs;
    }

    private static int parsePort(String host, String portStr) {
        try {
            int port = Integer.parseInt(portStr);
            if (port < 1 || port > 65535) {
                System.err.println("[ERROR] 端口号超出范围 (1-65535): " + portStr + " (host=" + host + ")");
                return -1;
            }
            return port;
        } catch (NumberFormatException e) {
            System.err.println("[ERROR] 端口号无效: " + portStr + " (host=" + host + ")");
            return -1;
        }
    }

    static void printConcurrentUsage() {
        System.err.println("并发模式用法:");
        System.err.println("  配置文件: java -jar ftp-client.jar --concurrent connections.txt");
        System.err.println("  同一主机多用户: java -jar ftp-client.jar --concurrent host port user1 pass1 user2 pass2 ...");
        System.err.println("  多主机: java -jar ftp-client.jar --concurrent host1 port1 user1 pass1 host2 port2 user2 pass2 ...");
        System.err.println("");
        System.err.println("connections.txt 格式 (每行一个连接，# 开头为注释):");
        System.err.println("  host port username password");
    }
}
