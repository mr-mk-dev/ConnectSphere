package me.manishcodes.connectsphere.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;
import me.manishcodes.connectsphere.enums.Passion;

import java.time.LocalDate;

@Data
public class UpdateProfileRequest {

    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    private String username;

    @Size(max = 500, message = "Bio must not exceed 500 characters")
    private String bio;

    private String profileUrl;

    private LocalDate dateOfBirth;

    private Passion passion;
}
