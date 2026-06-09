package me.manishcodes.connectsphere.dto.response;

import lombok.Builder;
import lombok.Data;
import me.manishcodes.connectsphere.enums.Passion;

import java.util.UUID;

@Data
@Builder
public class FollowResponse {
    private UUID id;
    private String username;
    private String profileUrl;
    private String bio;
    private Passion passion;
    private boolean isVerified;
}
