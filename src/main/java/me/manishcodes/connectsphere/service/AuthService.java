package me.manishcodes.connectsphere.service;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import me.manishcodes.connectsphere.dto.request.LoginRequest;
import me.manishcodes.connectsphere.dto.request.RefreshTokenRequest;
import me.manishcodes.connectsphere.dto.request.RegisterRequest;
import me.manishcodes.connectsphere.dto.response.AuthResponse;
import me.manishcodes.connectsphere.entity.User;
import me.manishcodes.connectsphere.enums.AuthProvider;
import me.manishcodes.connectsphere.enums.Role;
import me.manishcodes.connectsphere.exception.DuplicateResourceException;
import me.manishcodes.connectsphere.exception.ResourceNotFoundException;
import me.manishcodes.connectsphere.exception.UnauthorizedException;
import me.manishcodes.connectsphere.repository.UserRepository;
import me.manishcodes.connectsphere.security.JwtTokenProvider;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final RedisTemplate<String, String> redisTemplate;
    private final EmailService emailService;

    public String register(RegisterRequest request) {
        // 1. Check duplicates
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email is already registered");
        }
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateResourceException("Username is already taken");
        }

        // 2. Create user
        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.USER);
        user.setAuthProvider(AuthProvider.LOCAL);
        user.setVerified(false);
        user.setActive(true);
        User savedUser = userRepository.save(user);

        // 3. Generate email verification token → store in Redis for 24 hours
        String verifyToken = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set(
                "email-verify:" + verifyToken,
                savedUser.getId().toString(),
                Duration.ofHours(24)
        );

        // 4. Send verification email (async — doesn't block this response)
        emailService.sendVerificationEmail(
                savedUser.getEmail(),
                savedUser.getUsername(),
                verifyToken
        );

        return "Registration successful! Please check your email to verify your account.";
    }

    /**
     * Called when user clicks the verification link in their email.
     * Looks up the token in Redis → marks user as verified → deletes token.
     */
    public String verifyEmail(String token) {
        String redisKey = "email-verify:" + token;

        // 1. Look up the userId stored against this token in Redis
        String userIdStr = redisTemplate.opsForValue().get(redisKey);
        if (userIdStr == null) {
            throw new UnauthorizedException("Verification link is invalid or has expired.");
        }

        // 2. Find the user in DB
        UUID userId = UUID.fromString(userIdStr);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // 3. Mark as verified
        user.setVerified(true);
        userRepository.save(user);

        // 4. Delete token from Redis so the link can't be reused
        redisTemplate.delete(redisKey);

        return "Email verified successfully! You can now log in.";
    }

    public AuthResponse login(@Valid LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow( () -> new UnauthorizedException("Invalid email or password"));

        if(!passwordEncoder.matches(request.getPassword(),user.getPassword())){
            throw  new UnauthorizedException("Password is incorrect");
        }

        if(!user.isActive()){
            throw  new UnauthorizedException("Your Account is Banned");
        }

        if(!user.isVerified()){
            throw new UnauthorizedException("Your not verified yet, Verify it by requesting a verification link");
        }

        String accessToken = tokenProvider.generateAccessToken(
                user.getId(), user.getEmail(), user.getRole().name());

        String refreshToken = tokenProvider.generateRefreshToken(user.getId());

        return new AuthResponse(accessToken,refreshToken,"User Logged In Successful");

    }

    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();
        if (!tokenProvider.validateToken(refreshToken)) {
            throw new UnauthorizedException("Invalid or expired refresh token");
        }
        UUID userId = tokenProvider.getUserIdFromToken(refreshToken);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        String newAccessToken = tokenProvider.generateAccessToken(
                user.getId(), user.getEmail(), user.getRole().name());
        return new AuthResponse(newAccessToken, refreshToken, "Token refreshed");
    }


}
