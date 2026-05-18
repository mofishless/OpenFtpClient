# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

FTP 连通性测试工具 — 基于 JDK 8 的命令行工具，测试 FTP 服务器连通性。支持单连接模式和并发多连接保活模式。

## Build & Run

```bash
# 构建 fat jar
mvn clean package

# 运行单连接测试
java -jar target/ftp-client-1.0.0.jar <host> <port> <username> <password>

# 运行并发模式
java -jar target/ftp-client-1.0.0.jar --concurrent connections.txt

# 运行测试
mvn test

# 运行单个测试类
mvn test -Dtest=FtpConnectTesterTest
```

产物: `target/ftp-client-1.0.0.jar`（通过 maven-assembly-plugin 打包的 fat jar）。

## Architecture

入口类 `com.example.ftp.FtpConnectTester.main()` 根据是否传入 `--concurrent` 参数分发到两种模式：

- **单连接模式** (`FtpConnectTester`): 连接 FTP → 列出根目录 → 断开，返回退出码 0/1
- **并发模式** (`FtpConcurrentTester`): 解析连接配置 → 创建 `FtpClientWorker` 线程池 → 每个连接保持并每 30s 发送 NOOP 心跳 → shutdown hook 优雅断开

核心类职责：
- `ConnectionConfig` — 不可变连接参数 (host/port/username/password)
- `FtpClientWorker` — Runnable，封装单个 FTP 连接的完整生命周期（连接、列表、保活、断开）
- `FtpConcurrentTester` — 参数解析（配置文件/命令行）和线程池管理

## Key Constraints

- 目标 JDK 8，不能使用 var、record、text blocks 等高版本特性
- 使用 Apache Commons Net 3.9.0 的 `FTPClient`
- 测试框架为 JUnit 4（非 JUnit 5）
- 所有连接使用被动模式 (PASV)
- 超时配置：连接 10s，数据 30s，心跳间隔 30s
