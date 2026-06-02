package me.manishcodes.connectsphere.repository;

import me.manishcodes.connectsphere.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CommentRepository extends JpaRepository<Comment, UUID> {
}
