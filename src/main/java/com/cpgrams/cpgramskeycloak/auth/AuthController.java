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

}
