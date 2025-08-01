package com.cpgrams.cpgramskeycloak.auth;

import com.cpgrams.cpgramskeycloak.dto.RegistrationRequest;
import jakarta.ws.rs.core.Response;
import org.keycloak.admin.client.CreatedResponseUtil;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.RolesResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
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

    @Value("${keycloak.admin.username:admin}")
    private String adminUsername;

    @Value("${keycloak.admin.password:admin}")
    private String adminPassword;

    @Value("${keycloak.realm:cpgrams}")
    private String realm;

    public Map<String, Object> login(String username, String password) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("grant_type", "password");
            body.add("client_id", clientId);
            body.add("client_secret", clientSecret);

            if (username.matches("^\\d{10}$")) {
                body.add("mobile", username);
                body.add("otp", password);
            } else{
                body.add("username", username);
                body.add("password", password);
            }

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

                // Simple user data for demo
                result.put("username", username);
                result.put("email", username);
                result.put("firstName", "Demo");
                result.put("lastName", "User");
                result.put("role", "user");

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

    public Map<String, Object> registerUser(RegistrationRequest registrationRequest) {
        try {

            Keycloak keycloak = KeycloakBuilder.builder()
                    .serverUrl("http://localhost:8080")
                    .realm("master")
                    .username(adminUsername)
                    .password(adminPassword)
                    .clientId("admin-cli")
                    .build();


            UsersResource usersResource = keycloak.realm(realm).users();
            List<UserRepresentation> existingUsers = usersResource.search(registrationRequest.getUsername());

            if (!existingUsers.isEmpty()) {
                return createErrorResponse("Registration failed", "Username already exists");
            }


            List<UserRepresentation> existingEmails = usersResource.search(null, null, null, registrationRequest.getEmail(), null, null);

            if (!existingEmails.isEmpty()) {
                return createErrorResponse("Registration failed", "Email already exists");
            }


            UserRepresentation user = new UserRepresentation();
            user.setEnabled(true);
            user.setUsername(registrationRequest.getUsername());
            user.setEmail(registrationRequest.getEmail());
            user.setFirstName(registrationRequest.getFirstName());
            user.setLastName(registrationRequest.getLastName());
            user.setEmailVerified(true);


            CredentialRepresentation credential = new CredentialRepresentation();
            credential.setType(CredentialRepresentation.PASSWORD);
            credential.setValue(registrationRequest.getPassword());
            credential.setTemporary(false);
            user.setCredentials(List.of(credential));


            Response response = usersResource.create(user);

            if (response.getStatus() == 201) {
                // User created successfully
                String userId = CreatedResponseUtil.getCreatedId(response);

                // Assign the specified role (defaults to "user")
                assignRoleToUser(keycloak, userId, registrationRequest.getRole());

                Map<String, Object> result = new HashMap<>();
                result.put("status", "success");
                result.put("message", "User registered successfully");
                result.put("userId", userId);
                result.put("username", registrationRequest.getUsername());
                result.put("email", registrationRequest.getEmail());
                result.put("role", registrationRequest.getRole());

                return result;
            } else {
                String errorMessage = response.readEntity(String.class);
                return createErrorResponse("Registration failed", "Failed to create user: " + errorMessage);
            }

        } catch (Exception e) {
            return createErrorResponse("Registration failed", "Unexpected error: " + e.getMessage());
        }
    }

    private void assignRoleToUser(Keycloak keycloak, String userId, String roleName) {
        try {

            RolesResource rolesResource = keycloak.realm(realm).roles();
            var role = rolesResource.get(roleName).toRepresentation();

            if (role != null) {

                keycloak.realm(realm).users().get(userId).roles().realmLevel()
                        .add(List.of(role));
            } else {

                var defaultRole = rolesResource.get("user").toRepresentation();
                if (defaultRole != null) {
                    keycloak.realm(realm).users().get(userId).roles().realmLevel()
                            .add(List.of(defaultRole));
                }
            }
        } catch (Exception e) {

            System.err.println("Failed to assign role '" + roleName + "' to user: " + e.getMessage());


            try {
                var defaultRole = keycloak.realm(realm).roles().get("user").toRepresentation();
                if (defaultRole != null) {
                    keycloak.realm(realm).users().get(userId).roles().realmLevel()
                            .add(List.of(defaultRole));
                }
            } catch (Exception fallbackException) {
                System.err.println("Failed to assign default role: " + fallbackException.getMessage());
            }
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