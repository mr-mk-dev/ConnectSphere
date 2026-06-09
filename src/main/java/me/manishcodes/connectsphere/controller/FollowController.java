package me.manishcodes.connectsphere.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import me.manishcodes.connectsphere.dto.response.FollowResponse;
import me.manishcodes.connectsphere.service.FollowService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
@Tag(
    name = "Follow",
    description = "Manage follow relationships — follow/unfollow users and browse follower/following lists"
)
@SecurityRequirement(name = "bearerAuth")
public class FollowController {

    private final FollowService followService;


    @Operation(
        summary = "Follow a user",
        description = "The authenticated user follows the user identified by `{id}`. " +
                      "Fails with **409** if already following, **400** if trying to follow yourself."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully followed the user"),
        @ApiResponse(responseCode = "400", description = "Cannot follow yourself",
                     content = @Content(schema = @Schema(implementation = me.manishcodes.connectsphere.dto.response.ApiResponse.class))),
        @ApiResponse(responseCode = "401", description = "Not authenticated — JWT token required",
                     content = @Content(schema = @Schema(implementation = me.manishcodes.connectsphere.dto.response.ApiResponse.class))),
        @ApiResponse(responseCode = "404", description = "Target user not found",
                     content = @Content(schema = @Schema(implementation = me.manishcodes.connectsphere.dto.response.ApiResponse.class))),
        @ApiResponse(responseCode = "409", description = "Already following this user",
                     content = @Content(schema = @Schema(implementation = me.manishcodes.connectsphere.dto.response.ApiResponse.class)))
    })
    @PostMapping("/{id}/follow")
    public ResponseEntity<me.manishcodes.connectsphere.dto.response.ApiResponse<String>> followUser(
            @Parameter(description = "UUID of the user to follow", required = true,
                       example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
            @PathVariable UUID id,
            Authentication authentication) {

        String message = followService.followUser(id, authentication);
        return ResponseEntity.ok(me.manishcodes.connectsphere.dto.response.ApiResponse.success(message));
    }


    @Operation(
        summary = "Unfollow a user",
        description = "The authenticated user unfollows the user identified by `{id}`. " +
                      "Fails with **404** if the follow relationship does not exist."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successfully unfollowed the user"),
        @ApiResponse(responseCode = "401", description = "Not authenticated — JWT token required",
                     content = @Content(schema = @Schema(implementation = me.manishcodes.connectsphere.dto.response.ApiResponse.class))),
        @ApiResponse(responseCode = "404", description = "Not following this user, or user not found",
                     content = @Content(schema = @Schema(implementation = me.manishcodes.connectsphere.dto.response.ApiResponse.class)))
    })
    @DeleteMapping("/{id}/follow")
    public ResponseEntity<me.manishcodes.connectsphere.dto.response.ApiResponse<String>> unfollowUser(
            @Parameter(description = "UUID of the user to unfollow", required = true,
                       example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
            @PathVariable UUID id,
            Authentication authentication) {

        String message = followService.unfollowUser(id, authentication);
        return ResponseEntity.ok(me.manishcodes.connectsphere.dto.response.ApiResponse.success(message));
    }


    @Operation(
        summary = "Get followers",
        description = "Returns a paginated list of users who follow the user identified by `{id}`. " +
                      "Sorted by follow date — newest followers first. **Public endpoint, no auth required.**"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Paginated list of followers returned successfully"),
        @ApiResponse(responseCode = "404", description = "User not found",
                     content = @Content(schema = @Schema(implementation = me.manishcodes.connectsphere.dto.response.ApiResponse.class)))
    })
    @GetMapping("/{id}/followers")
    public ResponseEntity<me.manishcodes.connectsphere.dto.response.ApiResponse<Page<FollowResponse>>> getFollowers(
            @Parameter(description = "UUID of the user whose followers to fetch", required = true,
                       example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
            @PathVariable UUID id,

            @Parameter(description = "Page number, 0-indexed", example = "0")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Number of items per page (recommended max: 100)", example = "20")
            @RequestParam(defaultValue = "20") int size) {

        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<FollowResponse> followers = followService.getFollowers(id, pageable);
        return ResponseEntity.ok(me.manishcodes.connectsphere.dto.response.ApiResponse.success(followers));
    }


    @Operation(
        summary = "Get following",
        description = "Returns a paginated list of users that the user identified by `{id}` is following. " +
                      "Sorted by follow date — most recently followed first. **Public endpoint, no auth required.**"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Paginated list of following returned successfully"),
        @ApiResponse(responseCode = "404", description = "User not found",
                     content = @Content(schema = @Schema(implementation = me.manishcodes.connectsphere.dto.response.ApiResponse.class)))
    })
    @GetMapping("/{id}/following")
    public ResponseEntity<me.manishcodes.connectsphere.dto.response.ApiResponse<Page<FollowResponse>>> getFollowing(
            @Parameter(description = "UUID of the user whose following list to fetch", required = true,
                       example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
            @PathVariable UUID id,

            @Parameter(description = "Page number, 0-indexed", example = "0")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Number of items per page (recommended max: 100)", example = "20")
            @RequestParam(defaultValue = "20") int size) {

        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<FollowResponse> following = followService.getFollowing(id, pageable);
        return ResponseEntity.ok(me.manishcodes.connectsphere.dto.response.ApiResponse.success(following));
    }


    @Operation(
        summary = "Check follow status",
        description = "Returns `true` if the authenticated user currently follows the user identified by `{id}`, " +
                      "`false` otherwise. Use this to toggle the Follow/Unfollow button state on the frontend."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Follow status returned — true or false"),
        @ApiResponse(responseCode = "401", description = "Not authenticated — JWT token required",
                     content = @Content(schema = @Schema(implementation = me.manishcodes.connectsphere.dto.response.ApiResponse.class)))
    })
    @GetMapping("/{id}/is-following")
    public ResponseEntity<me.manishcodes.connectsphere.dto.response.ApiResponse<Boolean>> isFollowing(
            @Parameter(description = "UUID of the target user to check", required = true,
                       example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
            @PathVariable UUID id,
            Authentication authentication) {

        boolean result = followService.isFollowing(id, authentication);
        return ResponseEntity.ok(me.manishcodes.connectsphere.dto.response.ApiResponse.success(result));
    }
}
