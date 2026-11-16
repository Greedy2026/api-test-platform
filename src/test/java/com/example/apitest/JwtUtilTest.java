package com.example.apitest;

import com.example.apitest.security.JwtUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    @Test
    void generateAndParseRoundTrip() {
        JwtUtil jwtUtil = new JwtUtil("test-secret-key-0123456789-abcdef-0123456789", 1);
        String token = jwtUtil.generate("alice");
        assertNotNull(token);
        assertEquals("alice", jwtUtil.parseUsername(token));
    }

    @Test
    void tamperedTokenIsRejected() {
        JwtUtil jwtUtil = new JwtUtil("test-secret-key-0123456789-abcdef-0123456789", 1);
        String token = jwtUtil.generate("alice");
        assertNull(jwtUtil.parseUsername(token + "x"));
    }

    @Test
    void expiredTokenIsRejected() {
        // ttl-hours 为 0 等价于立即过期（剩余 0 毫秒）
        JwtUtil jwtUtil = new JwtUtil("test-secret-key-0123456789-abcdef-0123456789", 0);
        String token = jwtUtil.generate("bob");
        assertNull(jwtUtil.parseUsername(token));
    }
}
