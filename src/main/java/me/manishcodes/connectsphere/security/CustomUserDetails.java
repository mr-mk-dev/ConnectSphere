package me.manishcodes.connectsphere.security;

import me.manishcodes.connectsphere.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public class CustomUserDetails implements UserDetails {

    private UUID id;
    private String email;
    private String password;
    private boolean isActive;
    private boolean isVerified;
    private Collection<? extends GrantedAuthority> authorities;

    public CustomUserDetails(User user) {
        this.id = user.getId();
        this.email = user.getEmail();
        this.password = user.getPassword();
        this.isActive = user.isActive();
        this.isVerified = user.isVerified();
        // "USER" → "ROLE_USER" (Spring Security needs ROLE_ prefix)
        this.authorities = List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
    }

    public UUID getId() {
        return id;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;  // we login with email
    }

    @Override
    public boolean isAccountNonLocked() {
        return isActive;  // banned user = locked
    }

    @Override
    public boolean isEnabled() {
        return isVerified;  // unverified = disabled
    }
}
