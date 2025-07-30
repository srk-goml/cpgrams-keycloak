package com.cpgrams.cpgramskeycloak.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class GoogleOAuth2Service {

    @Autowired
    private RestTemplate restTemplate;

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String clientId;

    @Value("${spring.security.oauth2.client.registration.google.client-secret}")
    private String clientSecret;

    public Map<String, Object> exchangeCodeForToken(String authorizationCode, String redirectUri) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("grant_type", "authorization_code");
            body.add("client_id", clientId);
            body.add("client_secret", clientSecret);
            body.add("code", authorizationCode);
            body.add("redirect_uri", redirectUri);

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    "https://oauth2.googleapis.com/token", 
                    request, 
                    Map.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> tokenResponse = response.getBody();
                
                Map<String, Object> result = new HashMap<>();
                result.put("access_token", tokenResponse.get("access_token"));
                result.put("refresh_token", tokenResponse.get("refresh_token"));
                result.put("token_type", tokenResponse.get("token_type"));
                result.put("expires_in", tokenResponse.get("expires_in"));
                result.put("scope", tokenResponse.get("scope"));
                result.put("status", "success");
                result.put("message", "Google OAuth2 login successful");

                return result;
            } else {
                return createErrorResponse("Google OAuth2 failed", "Invalid response from Google");
            }
        } catch (Exception e) {
            return createErrorResponse("Google OAuth2 failed", "Error: " + e.getMessage());
        }
    }

    public Map<String, Object> getUserInfo(String accessToken) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);

            HttpEntity<String> request = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    "https://www.googleapis.com/oauth2/v2/userinfo",
                    HttpMethod.GET,
                    request,
                    Map.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> userInfo = response.getBody();
                
                Map<String, Object> result = new HashMap<>();
                result.put("id", userInfo.get("id"));
                result.put("email", userInfo.get("email"));
                result.put("name", userInfo.get("name"));
                result.put("given_name", userInfo.get("given_name"));
                result.put("family_name", userInfo.get("family_name"));
                result.put("picture", userInfo.get("picture"));
                result.put("verified_email", userInfo.get("verified_email"));
                result.put("status", "success");

                return result;
            } else {
                return createErrorResponse("Failed to get user info", "Invalid response from Google");
            }
        } catch (Exception e) {
            return createErrorResponse("Failed to get user info", "Error: " + e.getMessage());
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

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    "https://oauth2.googleapis.com/token", 
                    request, 
                    Map.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> tokenResponse = response.getBody();
                
                Map<String, Object> result = new HashMap<>();
                result.put("access_token", tokenResponse.get("access_token"));
                result.put("token_type", tokenResponse.get("token_type"));
                result.put("expires_in", tokenResponse.get("expires_in"));
                result.put("scope", tokenResponse.get("scope"));
                result.put("status", "success");
                result.put("message", "Token refreshed successfully");

                return result;
            } else {
                return createErrorResponse("Token refresh failed", "Invalid response from Google");
            }
        } catch (Exception e) {
            return createErrorResponse("Token refresh failed", "Error: " + e.getMessage());
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