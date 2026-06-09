package me.manishcodes.connectsphere.service;

import lombok.RequiredArgsConstructor;
import me.manishcodes.connectsphere.dto.response.FollowResponse;
import me.manishcodes.connectsphere.entity.Follow;
import me.manishcodes.connectsphere.entity.User;
import me.manishcodes.connectsphere.exception.DuplicateResourceException;
import me.manishcodes.connectsphere.exception.ResourceNotFoundException;
import me.manishcodes.connectsphere.repository.FollowRepository;
import me.manishcodes.connectsphere.repository.UserRepository;
import me.manishcodes.connectsphere.security.CustomUserDetails;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FollowService {

    private final FollowRepository followRepository;
    private final UserRepository userRepository;

    private UUID currentUserId(Authentication auth) {
        CustomUserDetails details = (CustomUserDetails) auth.getPrincipal();
        return details.getId();
    }


    private FollowResponse toFollowResponse(User user) {
        return FollowResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .profileUrl(user.getProfileUrl())
                .bio(user.getBio())
                .passion(user.getPassion())
                .isVerified(user.isVerified())
                .build();
    }


    @Transactional
    public String followUser(UUID targetUserId, Authentication auth) {
        UUID currentId = currentUserId(auth);

        if (currentId.equals(targetUserId)) {
            throw new IllegalArgumentException("You cannot follow yourself.");
        }

        if (followRepository.existsByFollowerIdAndFollowingId(currentId, targetUserId)) {
            throw new DuplicateResourceException("You are already following this user.");
        }

        User follower = userRepository.findById(currentId)
                .orElseThrow(() -> new ResourceNotFoundException("Current user not found."));
        User following = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Target user not found."));

        Follow follow = new Follow();
        follow.setFollower(follower);
        follow.setFollowing(following);
        followRepository.save(follow);

        return "You are now following @" + following.getUsername();
    }


    @Transactional
    public String unfollowUser(UUID targetUserId, Authentication auth) {
        UUID currentId = currentUserId(auth);

        Follow follow = followRepository
                .findByFollowerIdAndFollowingId(currentId, targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("You are not following this user."));

        followRepository.delete(follow);

        User target = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Target user not found."));

        return "You have unfollowed @" + target.getUsername();
    }


    public Page<FollowResponse> getFollowers(UUID userId, Pageable pageable) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found.");
        }
        return followRepository.findFollowersByUserId(userId, pageable)
                .map(this::toFollowResponse);
    }


    public Page<FollowResponse> getFollowing(UUID userId, Pageable pageable) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found.");
        }
        return followRepository.findFollowingByUserId(userId, pageable)
                .map(this::toFollowResponse);
    }


    public boolean isFollowing(UUID targetUserId, Authentication auth) {
        UUID currentId = currentUserId(auth);
        return followRepository.existsByFollowerIdAndFollowingId(currentId, targetUserId);
    }
}
