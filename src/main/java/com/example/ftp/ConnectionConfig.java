package com.example.ftp;

public final class ConnectionConfig {
    final String host;
    final int port;
    final String username;
    final String password;

    ConnectionConfig(String host, int port, String username, String password) {
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
    }

    @Override
    public String toString() {
        return host + ":" + port + "@" + username;
    }
}
