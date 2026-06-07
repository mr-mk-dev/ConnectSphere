package me.manishcodes.connectsphere.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import me.manishcodes.connectsphere.dto.request.UpdateProfileRequest;
import me.manishcodes.connectsphere.dto.response.ApiResponse;
import me.manishcodes.connectsphere.dto.response.UserResponse;
import me.manishcodes.connectsphere.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import org.springframework.web.bind.annotation.*;

import java.util.UUID;

import org.springframework.data.domain.Page;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
@Tag(name = "Users", description = "View profiles, update your own profile and search users")
public class UserController {

    private final UserService userService;


    @Operation(summary = "Get my profile", description = "Returns the currently authenticated user's profile")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getMyProfile(Authentication authentication) {
        String email = authentication.getName();
        UserResponse response = userService.getMyProfile(email);
        return ResponseEntity.ok(ApiResponse.success(response));
    }


    @Operation(summary = "Update my profile", description = "Update bio, username, passion, date of birth and profile picture")
    @PutMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(
            @Valid @RequestBody UpdateProfileRequest request,
            Authentication authentication) {

        String email = authentication.getName();
        UserResponse response = userService.updateProfile(email, request);
        return ResponseEntity.ok(ApiResponse.success(200, "Profile updated successfully", response));
    }

    @Operation(summary = "Get user by ID", description = "Returns any user's public profile by their UUID")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable UUID id) {
        UserResponse userResponse = userService.gerUserById(id);
        return ResponseEntity.ok(ApiResponse.success(userResponse));
    }

    @Operation(summary = "Search users", description = "Search by username or email. Results are sorted by follower count (most popular first). Paginated — default 10 per page.")
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<Page<UserResponse>>> searchUsers(
            @RequestParam String q,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<UserResponse> results = userService.searchUsers(q, page, size);
        return ResponseEntity.ok(ApiResponse.success(results));
    }

}
