package me.manishcodes.connectsphere.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import me.manishcodes.connectsphere.dto.request.ForgotPasswordRequest;
import me.manishcodes.connectsphere.dto.request.LoginRequest;
import me.manishcodes.connectsphere.dto.request.RegisterRequest;
import me.manishcodes.connectsphere.dto.request.ResetPasswordRequest;
import me.manishcodes.connectsphere.dto.response.ApiResponse;
import me.manishcodes.connectsphere.dto.response.AuthResponse;
import me.manishcodes.connectsphere.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Register, login, email verification and password reset")
@SecurityRequirements   // all auth endpoints are public — no JWT needed
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "Register a new user", description = "Creates a new account and sends a verification email")
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<String>> register(
            @Valid @RequestBody RegisterRequest request)
    {
        String response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Registration Completed", response));
    }

    @Operation(summary = "Login", description = "Returns a JWT access token on success")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request)
    {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * GET /api/v1/auth/verify-email?token=<uuid>
     * This is the link the user clicks in their inbox.
     * Spring hits this endpoint, AuthService validates the token in Redis,
     * marks the user verified in DB, and deletes the token so it can't be reused.
     */
    @Operation(summary = "Verify email", description = "Click link from the verification email — validates the token and activates the account")
    @GetMapping("/verify-email")
    public ResponseEntity<ApiResponse<String>> verifyEmail(@RequestParam String token) {
        String message = authService.verifyEmail(token);
        return ResponseEntity.ok(ApiResponse.success(message));
    }

    @Operation(summary = "Forgot password", description = "Sends a password reset link to the user's email")
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<String>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        String message = authService.forgotPassword(request);
        return ResponseEntity.ok(ApiResponse.success(message));
    }

    @Operation(summary = "Reset password", description = "Resets the password using the token from the reset email")
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<String>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        String message = authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.success(message));
    }

}
