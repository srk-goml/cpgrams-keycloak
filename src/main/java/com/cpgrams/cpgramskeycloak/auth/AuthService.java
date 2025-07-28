package com.cpgrams.cpgramskeycloak.auth;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class AuthService {

    @Autowired
    private RestTemplate restTemplate;

    @Value("${spring.security.oauth2.client.registration.keycloak.client-id}")
    private String clientId;

    @Value("${spring.security.oauth2.client.registration.keycloak.client-secret}")
    private String clientSecret;

    @Value("${spring.security.oauth2.client.provider.keycloak.token-uri}")
    private String tokenUri;

    public Map<String, Object> login(String username, String password) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("grant_type", "password");
            body.add("client_id", clientId);
            body.add("client_secret", clientSecret);
            body.add("username", username);
            body.add("password", password);

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(tokenUri, request, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> tokenResponse = response.getBody();

                Map<String, Object> result = new HashMap<>();
                result.put("access_token", tokenResponse.get("access_token"));
                result.put("refresh_token", tokenResponse.get("refresh_token"));
                result.put("token_type", tokenResponse.get("token_type"));
                result.put("expires_in", tokenResponse.get("expires_in"));
                result.put("scope", tokenResponse.get("scope"));
                result.put("status", "success");
                result.put("message", "Login successful");

                return result;
            } else {
                return createErrorResponse("Login failed", "Invalid response from authentication server");
            }
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                return createErrorResponse("Login failed", "Invalid username or password");
            } else if (e.getStatusCode() == HttpStatus.BAD_REQUEST) {
                return createErrorResponse("Login failed", "Bad request - check your credentials");
            } else {
                return createErrorResponse("Login failed", "Authentication server error: " + e.getMessage());
            }
        } catch (Exception e) {
            return createErrorResponse("Login failed", "Unexpected error: " + e.getMessage());
        }
    }

    public Map<String, Object> refreshToken(String refreshToken) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("grant_type", "refresh_token");
            body.add("client_id", clientId);
            body.add("client_secret", clientSecret);
            body.add("refresh_token", refreshToken);

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(tokenUri, request, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> tokenResponse = response.getBody();

                Map<String, Object> result = new HashMap<>();
                result.put("access_token", tokenResponse.get("access_token"));
                result.put("refresh_token", tokenResponse.get("refresh_token"));
                result.put("token_type", tokenResponse.get("token_type"));
                result.put("expires_in", tokenResponse.get("expires_in"));
                result.put("status", "success");
                result.put("message", "Token refreshed successfully");

                return result;
            } else {
                return createErrorResponse("Token refresh failed", "Invalid response from authentication server");
            }
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                return createErrorResponse("Token refresh failed", "Invalid or expired refresh token");
            } else {
                return createErrorResponse("Token refresh failed", "Authentication server error: " + e.getMessage());
            }
        } catch (Exception e) {
            return createErrorResponse("Token refresh failed", "Unexpected error: " + e.getMessage());
        }
    }

    private Map<String, Object> createErrorResponse(String status, String message) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("status", "error");
        errorResponse.put("message", message);
        errorResponse.put("timestamp", System.currentTimeMillis());
        return errorResponse;
    }
}