package com.cpgrams.cpgramskeycloak.auth;

import com.cpgrams.cpgramskeycloak.dto.LoginRequest;
import com.cpgrams.cpgramskeycloak.dto.RefreshTokenRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;
    
    @Autowired
    private com.cpgrams.cpgramskeycloak.service.GoogleOAuth2Service googleOAuth2Service;

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody LoginRequest loginRequest) {
        if (loginRequest.getUsername() == null || loginRequest.getUsername().trim().isEmpty()) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "Username is required");
            return ResponseEntity.badRequest().body(errorResponse);
        }

        if (loginRequest.getPassword() == null || loginRequest.getPassword().trim().isEmpty()) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "Password is required");
            return ResponseEntity.badRequest().body(errorResponse);
        }

        Map<String, Object> response = authService.login(loginRequest.getUsername(), loginRequest.getPassword());

        if ("success".equals(response.get("status"))) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<Map<String, Object>> refreshToken(@RequestBody RefreshTokenRequest refreshTokenRequest) {
        if (refreshTokenRequest.getRefreshToken() == null || refreshTokenRequest.getRefreshToken().trim().isEmpty()) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "Refresh token is required");
            return ResponseEntity.badRequest().body(errorResponse);
        }

        Map<String, Object> response = authService.refreshToken(refreshTokenRequest.getRefreshToken());

        if ("success".equals(response.get("status"))) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
    }

    @GetMapping("/user-info")
    public ResponseEntity<Map<String, Object>> getUserInfo(@AuthenticationPrincipal Jwt jwt) {
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("sub", jwt.getSubject());
        userInfo.put("username", jwt.getClaimAsString("preferred_username"));
        userInfo.put("email", jwt.getClaimAsString("email"));
        userInfo.put("firstName", jwt.getClaimAsString("given_name"));
        userInfo.put("lastName", jwt.getClaimAsString("family_name"));

        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess != null && realmAccess.get("roles") != null) {
            userInfo.put("roles", realmAccess.get("roles"));
        }

        return ResponseEntity.ok(userInfo);
    }

    @GetMapping("/validate-token")
    public ResponseEntity<Map<String, String>> validateToken(@AuthenticationPrincipal Jwt jwt) {
        Map<String, String> response = new HashMap<>();
        response.put("status", "valid");
        response.put("user", jwt.getClaimAsString("preferred_username"));
        response.put("expires_at", jwt.getExpiresAt().toString());

        return ResponseEntity.ok(response);
    }

    // Google OAuth2 endpoints
    @GetMapping("/google/login")
    public ResponseEntity<Map<String, Object>> googleLogin() {
        String googleAuthUrl = "https://accounts.google.com/o/oauth2/v2/auth?" +
                "client_id=" + "232653743705-19nrvan4e4q5qa67eut3qpbq740l8va4.apps.googleusercontent.com" +
                "&redirect_uri=" + "http://localhost:8082/oauth2/callback/google" +
                "&response_type=code" +
                "&scope=email profile" +
                "&access_type=offline";

        Map<String, Object> response = new HashMap<>();
        response.put("auth_url", googleAuthUrl);
        response.put("message", "Redirect user to this URL for Google OAuth2 login");

        return ResponseEntity.ok(response);
    }

    @GetMapping("/oauth2/callback/google")
    public ResponseEntity<Map<String, Object>> googleCallback(@RequestParam("code") String code) {
        String redirectUri = "http://localhost:8082/oauth2/callback/google";
        Map<String, Object> tokenResponse = googleOAuth2Service.exchangeCodeForToken(code, redirectUri);

        if ("success".equals(tokenResponse.get("status"))) {
            // Get user info from Google
            String accessToken = (String) tokenResponse.get("access_token");
            Map<String, Object> userInfo = googleOAuth2Service.getUserInfo(accessToken);
            
            // Combine token and user info
            Map<String, Object> response = new HashMap<>(tokenResponse);
            response.put("user_info", userInfo);
            
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(tokenResponse);
        }
    }

    @PostMapping("/google/refresh")
    public ResponseEntity<Map<String, Object>> googleRefreshToken(@RequestBody GoogleRefreshRequest refreshRequest) {
        if (refreshRequest.getRefreshToken() == null || refreshRequest.getRefreshToken().trim().isEmpty()) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "Refresh token is required");
            return ResponseEntity.badRequest().body(errorResponse);
        }

        Map<String, Object> response = googleOAuth2Service.refreshToken(refreshRequest.getRefreshToken());

        if ("success".equals(response.get("status"))) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
    }

    @GetMapping("/google/user-info")
    public ResponseEntity<Map<String, Object>> googleUserInfo(@RequestHeader("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "Bearer token is required");
            return ResponseEntity.badRequest().body(errorResponse);
        }

        String accessToken = authHeader.substring(7);
        Map<String, Object> response = googleOAuth2Service.getUserInfo(accessToken);

        if ("success".equals(response.get("status"))) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
    }

    @GetMapping("/oauth2-success")
    public ResponseEntity<Map<String, Object>> oauth2Success() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "OAuth2 login successful");
        response.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/oauth2-failure")
    public ResponseEntity<Map<String, Object>> oauth2Failure() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "error");
        response.put("message", "OAuth2 login failed");
        response.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    // Inner classes for request bodies
    public static class LoginRequest {
        private String username;
        private String password;

        public LoginRequest() {}

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }

        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    public static class RefreshTokenRequest {
        private String refreshToken;

        public RefreshTokenRequest() {}

        public String getRefreshToken() { return refreshToken; }
        public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
    }

    public static class GoogleRefreshRequest {
        private String refreshToken;

        public GoogleRefreshRequest() {}

        public String getRefreshToken() { return refreshToken; }
        public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
    }
}
