package com.example.apitest.controller;

import com.example.apitest.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    public record Credentials(String username, String password) {}

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Credentials body) {
        try {
            return ResponseEntity.ok(authService.register(body.username(), body.password()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Credentials body) {
        try {
            return ResponseEntity.ok(authService.login(body.username(), body.password()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
