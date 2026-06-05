package me.manishcodes.connectsphere.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ResetPasswordRequest {

    @NotBlank(message = "Token is required for Password Reset")
    private String token;

    @Size(min = 3 , message = "Minimum Length should be at least 3")
    private String newPassword;
}
