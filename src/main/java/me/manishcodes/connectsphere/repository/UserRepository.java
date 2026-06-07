package me.manishcodes.connectsphere.repository;

import jakarta.validation.constraints.NotNull;
import me.manishcodes.connectsphere.entity.User;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    @NotNull Optional<User> findById(@NonNull UUID id);
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    boolean existsByUsername(String username);

    @Query("""
            SELECT u FROM User u
            WHERE LOWER(u.username) LIKE LOWER(CONCAT('%', :query, '%'))
               OR LOWER(u.email)    LIKE LOWER(CONCAT('%', :query, '%'))
            ORDER BY SIZE(u.followers) DESC
            """)
    Page<User> searchByUsernameOrEmail(@Param("query") String query, Pageable pageable);
}
