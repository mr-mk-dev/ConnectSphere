package me.manishcodes.connectsphere.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import me.manishcodes.connectsphere.dto.request.LoginRequest;
import me.manishcodes.connectsphere.dto.request.RegisterRequest;
import me.manishcodes.connectsphere.dto.response.ApiResponse;
import me.manishcodes.connectsphere.dto.response.AuthResponse;
import me.manishcodes.connectsphere.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<String>> register(
            @Valid @RequestBody RegisterRequest request)
    {
        String response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Registration Completed", response));
    }

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
    @GetMapping("/verify-email")
    public ResponseEntity<ApiResponse<String>> verifyEmail(@RequestParam String token) {
        String message = authService.verifyEmail(token);
        return ResponseEntity.ok(ApiResponse.success(message));
    }

}
