package com.cpgrams.cpgramskeycloak.controller;

import com.cpgrams.cpgramskeycloak.model.UserProfile;
import com.cpgrams.cpgramskeycloak.service.GoogleOAuth2Service;
import com.cpgrams.cpgramskeycloak.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class WebController {

    @Autowired
    private UserService userService;

    @Autowired
    private GoogleOAuth2Service googleOAuth2Service;

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String googleClientId;



    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("title", "CPGrams Keycloak Integration");
        model.addAttribute("description", "Spring Boot application with Keycloak authentication");
        return "home";
    }

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("registrationRequest", new com.cpgrams.cpgramskeycloak.dto.RegistrationRequest());
        return "register";
    }

    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        String username = (String) session.getAttribute("username");

        if (username != null) {
            model.addAttribute("username", username);
            model.addAttribute("email", session.getAttribute("email"));
            model.addAttribute("firstName", session.getAttribute("firstName"));
            model.addAttribute("lastName", session.getAttribute("lastName"));
        } else {
            return "redirect:/login";
        }

        return "dashboard";
    }

    @GetMapping("/admin")
    public String adminDashboard(HttpSession session, Model model) {
        String username = (String) session.getAttribute("username");
        String role = (String) session.getAttribute("role");

        if (username != null && "client_admin".equals(role)) {
            model.addAttribute("admin", username);

            // Get system info
            Map<String, Object> systemInfo = new HashMap<>();
            systemInfo.put("admin", username);
            systemInfo.put("system_status", "Active");
            systemInfo.put("total_users", userService.getAllUsers().size());
            systemInfo.put("server_time", System.currentTimeMillis());
            model.addAttribute("systemInfo", systemInfo);

            // Get all users
            List<UserProfile> users = userService.getAllUsers();
            model.addAttribute("users", users);
        } else {
            return "redirect:/login";
        }

        return "admin";
    }

    @GetMapping("/profile")
    public String profile(HttpSession session, Model model) {
        String username = (String) session.getAttribute("username");

        if (username != null) {
            model.addAttribute("username", username);
            model.addAttribute("email", session.getAttribute("email"));
            model.addAttribute("firstName", session.getAttribute("firstName"));
            model.addAttribute("lastName", session.getAttribute("lastName"));
        } else {
            return "redirect:/login";
        }

        return "profile";
    }

    // Add session endpoint for login success
    @PostMapping("/login-success")
    public String loginSuccess(HttpSession session,
                             @RequestParam String username,
                             @RequestParam String email,
                             @RequestParam String firstName,
                             @RequestParam String lastName,
                             @RequestParam String role) {

        session.setAttribute("username", username);
        session.setAttribute("email", email);
        session.setAttribute("firstName", firstName);
        session.setAttribute("lastName", lastName);
        session.setAttribute("role", role);

        return "redirect:/dashboard";
    }

    @GetMapping("/oauth2/google")
    public String googleOAuth2Login() {
        // Redirect directly to Google OAuth2 instead of the API endpoint
        return "redirect:https://accounts.google.com/o/oauth2/v2/auth?" +
               "client_id=" + googleClientId +
               "&redirect_uri=http://localhost:8082/oauth2/callback/google" +
               "&response_type=code" +
               "&scope=email profile" +
               "&access_type=offline";
    }

    @GetMapping("/oauth2-success")
    public String oauth2Success(Model model) {
        model.addAttribute("message", "OAuth2 login successful!");
        model.addAttribute("status", "success");
        return "oauth-result";
    }

    @GetMapping("/oauth2-failure")
    public String oauth2Failure(Model model) {
        model.addAttribute("message", "OAuth2 login failed!");
        model.addAttribute("status", "error");
        return "oauth-result";
    }

    @GetMapping("/api-docs")
    public String apiDocs(Model model) {
        model.addAttribute("baseUrl", "http://localhost:8082");
        return "api-docs";
    }

    @GetMapping("/oauth2/callback/google")
    public String googleOAuth2Callback(@RequestParam("code") String code, 
                                   HttpSession session, 
                                   Model model) {
    try {
        String redirectUri = "http://localhost:8082/oauth2/callback/google";
        
        // Exchange code for token
        Map<String, Object> tokenResponse = googleOAuth2Service.exchangeCodeForToken(code, redirectUri);
        
        if ("success".equals(tokenResponse.get("status"))) {
            // Get user info from Google
            String accessToken = (String) tokenResponse.get("access_token");
            Map<String, Object> userInfo = googleOAuth2Service.getUserInfo(accessToken);
            
            // Store in session
            session.setAttribute("access_token", accessToken);
            session.setAttribute("refresh_token", tokenResponse.get("refresh_token"));
            session.setAttribute("username", userInfo.get("email"));
            session.setAttribute("email", userInfo.get("email"));
            session.setAttribute("firstName", userInfo.get("given_name"));
            session.setAttribute("lastName", userInfo.get("family_name"));
            session.setAttribute("role", "user");
            
            // Store in model for the view
            model.addAttribute("access_token", accessToken);
            model.addAttribute("refresh_token", tokenResponse.get("refresh_token"));
            model.addAttribute("user_info", userInfo);
            
            return "oauth-callback";
        } else {
            // Handle error
            model.addAttribute("error", tokenResponse.get("message"));
            return "oauth-result";
        }
    } catch (Exception e) {
        model.addAttribute("error", "OAuth2 callback failed: " + e.getMessage());
        return "oauth-result";
    }
}
}