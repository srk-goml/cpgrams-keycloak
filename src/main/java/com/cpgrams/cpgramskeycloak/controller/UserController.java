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
import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping("/profile")
    @PreAuthorize("hasRole('client_admin')")
    public ResponseEntity<UserProfile> getUserProfile(@AuthenticationPrincipal Jwt jwt) {
        UserProfile userProfile = userService.createOrUpdateUserProfile(jwt);
        return ResponseEntity.ok(userProfile);
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('client_admin')")
    public ResponseEntity<Map<String, String>> getUserDashboard(@AuthenticationPrincipal Jwt jwt) {
        Map<String, String> dashboard = new HashMap<>();
        dashboard.put("message", "Welcome to user dashboard!");
        dashboard.put("user", jwt.getClaimAsString("preferred_username"));
        dashboard.put("access_level", "USER");

        return ResponseEntity.ok(dashboard);
    }
}