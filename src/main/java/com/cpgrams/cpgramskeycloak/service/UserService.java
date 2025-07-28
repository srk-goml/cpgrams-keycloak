package com.cpgrams.cpgramskeycloak.service;


import com.cpgrams.cpgramskeycloak.model.UserProfile;
import com.cpgrams.cpgramskeycloak.repo.UserProfileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private UserProfileRepository userProfileRepository;

    public UserProfile createOrUpdateUserProfile(Jwt jwt) {
        String keycloakUserId = jwt.getSubject();
        String username = jwt.getClaimAsString("preferred_username");
        String email = jwt.getClaimAsString("email");
        String firstName = jwt.getClaimAsString("given_name");
        String lastName = jwt.getClaimAsString("family_name");

        Optional<UserProfile> existingUser = userProfileRepository.findByKeycloakUserId(keycloakUserId);

        UserProfile userProfile;
        if (existingUser.isPresent()) {
            userProfile = existingUser.get();
            userProfile.setEmail(email);
            userProfile.setFirstName(firstName);
            userProfile.setLastName(lastName);
        } else {
            userProfile = new UserProfile(keycloakUserId, username, email);
            userProfile.setFirstName(firstName);
            userProfile.setLastName(lastName);
        }

        return userProfileRepository.save(userProfile);
    }

    public Optional<UserProfile> getUserByKeycloakId(String keycloakUserId) {
        return userProfileRepository.findByKeycloakUserId(keycloakUserId);
    }

    public List<UserProfile> getAllUsers() {
        return userProfileRepository.findAll();
    }
}