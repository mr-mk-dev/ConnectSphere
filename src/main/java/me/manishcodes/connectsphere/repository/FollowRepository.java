package me.manishcodes.connectsphere.repository;

import me.manishcodes.connectsphere.entity.Follow;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface FollowRepository extends JpaRepository<Follow, UUID> {

}
