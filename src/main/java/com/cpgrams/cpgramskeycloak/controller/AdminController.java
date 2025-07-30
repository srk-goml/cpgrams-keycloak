package com.cpgrams.cpgramskeycloak.controller;


import com.cpgrams.cpgramskeycloak.model.UserProfile;
import com.cpgrams.cpgramskeycloak.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired
    private UserService userService;

    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('client_admin')")
    public ResponseEntity<Map<String, String>> getAdminDashboard(@AuthenticationPrincipal Jwt jwt) {
        Map<String, String> dashboard = new HashMap<>();
        dashboard.put("message", "Welcome to admin dashboard!");
        dashboard.put("admin", jwt.getClaimAsString("preferred_username"));
        dashboard.put("access_level", "ADMIN");

        return ResponseEntity.ok(dashboard);
    }

    @GetMapping("/users")
    @PreAuthorize("hasRole('client_admin')")
    public ResponseEntity<List<UserProfile>> getAllUsers() {
        List<UserProfile> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    @GetMapping("/system-info")
    @PreAuthorize("hasRole('client_admin')")
    public ResponseEntity<Map<String, Object>> getSystemInfo(@AuthenticationPrincipal Jwt jwt) {
        Map<String, Object> systemInfo = new HashMap<>();
        systemInfo.put("admin", jwt.getClaimAsString("preferred_username"));
        systemInfo.put("system_status", "Active");
        systemInfo.put("total_users", userService.getAllUsers().size());
        systemInfo.put("server_time", System.currentTimeMillis());

        return ResponseEntity.ok(systemInfo);
    }
}
