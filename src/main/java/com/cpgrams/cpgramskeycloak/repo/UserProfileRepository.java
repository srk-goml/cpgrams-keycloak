package com.cpgrams.cpgramskeycloak.repo;

import com.cpgrams.cpgramskeycloak.model.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserProfileRepository extends JpaRepository<UserProfile,Long> {
    Optional<UserProfile> findByKeycloakUserId(String keycloakUserId);
    Optional<UserProfile> findByUsername(String username);
    Optional<UserProfile> findByEmail(String email);
}
