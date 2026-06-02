package me.manishcodes.connectsphere.repository;

import me.manishcodes.connectsphere.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PostRepository extends JpaRepository<Post, UUID> {
}
