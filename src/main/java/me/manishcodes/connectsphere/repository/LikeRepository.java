package me.manishcodes.connectsphere.repository;

import me.manishcodes.connectsphere.entity.Like;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface LikeRepository extends JpaRepository<Like, UUID> {
}
