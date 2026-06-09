package me.manishcodes.connectsphere.dto.response;

import lombok.Builder;
import lombok.Data;
import me.manishcodes.connectsphere.enums.AuthProvider;
import me.manishcodes.connectsphere.enums.Passion;
import me.manishcodes.connectsphere.enums.Role;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class UserResponse {
    private UUID id;
    private String username;
    private String email;
    private String bio;
    private String profileUrl;
    private LocalDate dateOfBirth;
    private Passion passion;
    private Role role;
    private AuthProvider authProvider;
    private boolean isVerified;
    private boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
