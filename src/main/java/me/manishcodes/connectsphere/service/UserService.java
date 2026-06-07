package me.manishcodes.connectsphere.service;

import lombok.RequiredArgsConstructor;
import me.manishcodes.connectsphere.dto.request.UpdateProfileRequest;
import me.manishcodes.connectsphere.dto.response.UserResponse;
import me.manishcodes.connectsphere.entity.User;
import me.manishcodes.connectsphere.exception.DuplicateResourceException;
import me.manishcodes.connectsphere.exception.ResourceNotFoundException;
import me.manishcodes.connectsphere.repository.UserRepository;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public UserResponse updateProfile(String email, UpdateProfileRequest request) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (request.getUsername() != null &&
                !request.getUsername().equals(user.getUsername())) {

            if (userRepository.existsByUsername(request.getUsername())) {
                throw new DuplicateResourceException(
                        "Username '" + request.getUsername() + "' is already taken");
            }

            user.setUsername(request.getUsername());
        }

        if (request.getBio() != null) {
            user.setBio(request.getBio());
        }
        if (request.getProfileUrl() != null) {
            user.setProfileUrl(request.getProfileUrl());
        }
        if (request.getDateOfBirth() != null) {
            user.setDateOfBirth(request.getDateOfBirth());
        }
        if (request.getPassion() != null) {
            user.setPassion(request.getPassion());
        }
        userRepository.save(user);

        return toUserResponse(user);
    }

    public UserResponse getMyProfile(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return toUserResponse(user);
    }

    private UserResponse toUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .bio(user.getBio())
                .profileUrl(user.getProfileUrl())
                .dateOfBirth(user.getDateOfBirth())
                .passion(user.getPassion())
                .role(user.getRole())
                .authProvider(user.getAuthProvider())
                .isVerified(user.isVerified())
                .isActive(user.isActive())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    public UserResponse gerUserById(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UsernameNotFoundException("User Not Found with " + id + " give user id"));
        return toUserResponse(user);
    }

    public Page<UserResponse> searchUsers(String query, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size);
        return userRepository
                .searchByUsernameOrEmail(query, pageable)
                .map(this::toUserResponse);
    }
}
