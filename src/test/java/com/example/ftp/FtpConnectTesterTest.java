package com.example.ftp;

import org.junit.Test;
import static org.junit.Assert.*;

public class FtpConnectTesterTest {

    @Test
    public void testInvalidPort_returnsError() {
        // 连接一个不存在的地址，期望返回 1（失败）
        int result = FtpConnectTester.testConnection("127.0.0.1", 19999, "user", "pass");
        assertEquals(1, result);
    }

    @Test
    public void testWrongCredentials_returnsError() {
        // 连接一个不存在的地址，期望返回 1（失败）
        int result = FtpConnectTester.testConnection("0.0.0.0", 21, "baduser", "badpass");
        assertEquals(1, result);
    }
}
