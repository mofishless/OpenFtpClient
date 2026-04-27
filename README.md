# FTP 连通性测试工具

基于 JDK 8 的命令行工具，用于测试 FTP 服务器连通性。连接成功后列出根目录内容，支持单连接和多客户端并发两种模式。

## 环境要求

- JDK 8+
- Maven 3.x（仅构建时需要）

## 构建

```bash
mvn clean package
```

生成 `target/ftp-client-1.0.0.jar`（fat jar，无需额外依赖）。

---

## 使用方式

### 单连接模式

连接一个 FTP 服务器，列出根目录后自动断开，退出码标识结果。

```bash
java -jar ftp-client-1.0.0.jar <host> <port> <username> <password>
```

**示例：**

```bash
java -jar ftp-client-1.0.0.jar 192.168.1.100 21 ftpuser mypassword
```

**输出示例：**

```
[INFO] 正在连接 192.168.1.100:21 ...
[INFO] 登录成功
[INFO] 连接模式: 被动模式 (PASV)
[INFO] 被动模式数据端口: 54321 (服务器地址: 192.168.1.100)
[INFO] 根目录文件列表:
  [DIR]  upload
  [DIR]  backup
  [FILE] readme.txt
[INFO] 连接测试成功，共 3 个条目
```

**退出码：**

| 退出码 | 含义 |
|--------|------|
| `0` | 连接成功 |
| `1` | 连接失败 |

可在脚本中直接判断：

```bash
java -jar ftp-client-1.0.0.jar 192.168.1.100 21 user pass
if [ $? -eq 0 ]; then
  echo "FTP 可达"
fi
```

---

### 并发模式

同时建立多个 FTP 连接，连接后保持不断开，每 30 秒发送 NOOP 心跳维持会话，按 `Ctrl+C` 后优雅断开所有连接。

#### 方式一：配置文件

```bash
java -jar ftp-client-1.0.0.jar --concurrent connections.txt
```

`connections.txt` 格式（每行一个连接，`#` 开头为注释）：

```
# 格式: host port username password
192.168.1.100 21 user1 pass1
192.168.1.100 21 user2 pass2
192.168.1.101 21 user3 pass3
```

#### 方式二：同一主机，多个用户

```bash
java -jar ftp-client-1.0.0.jar --concurrent <host> <port> <user1> <pass1> <user2> <pass2> ...
```

**示例：**

```bash
java -jar ftp-client-1.0.0.jar --concurrent 192.168.1.100 21 user1 pass1 user2 pass2
```

#### 方式三：多个主机

每组四个参数 `host port username password`，可重复：

```bash
java -jar ftp-client-1.0.0.jar --concurrent \
  192.168.1.100 21 user1 pass1 \
  192.168.1.101 21 user2 pass2
```

**并发模式输出示例：**

```
[INFO] 启动 2 个并发 FTP 连接...
[INFO][Client-1] 正在连接 192.168.1.100:21 ...
[INFO][Client-2] 正在连接 192.168.1.100:21 ...
[INFO][Client-1] 登录成功
[INFO][Client-1] 连接模式: 被动模式 (PASV)
[INFO][Client-1] 被动模式数据端口: 54321 (服务器地址: 192.168.1.100)
[INFO][Client-1] 根目录文件列表:
  [Client-1] [DIR]  upload
  [Client-1] [FILE] readme.txt
[INFO][Client-1] 目录列表完成，共 2 个条目，连接保持中（每 30s 发送 NOOP）...
[INFO][Client-2] 登录成功
...
^C
[INFO] 收到退出信号，正在断开所有连接...
[INFO][Client-1] 已断开连接
[INFO][Client-2] 已断开连接
[INFO] 所有连接已断开
```

---

## 超时设置

| 参数 | 默认值 | 说明 |
|------|--------|------|
| 连接超时 | 10 秒 | TCP 连接建立超时 |
| 数据超时 | 30 秒 | 数据传输超时 |
| NOOP 心跳间隔 | 30 秒 | 并发模式保活间隔 |
