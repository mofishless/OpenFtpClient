package com.example.ftp;

import org.junit.Test;
import static org.junit.Assert.*;

public class FtpUploaderTest {

    @Test
    public void testInvalidFilePath_returnsError() {
        int result = FtpUploader.upload("192.168.1.100", 21, "user", "pass", "/nonexistent/path/file.txt");
        assertEquals(1, result);
    }

    @Test
    public void testUnreachableServer_returnsError() {
        int result = FtpUploader.upload("127.0.0.1", 19999, "user", "pass", "pom.xml");
        assertEquals(1, result);
    }
}
