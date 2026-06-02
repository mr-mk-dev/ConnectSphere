package me.manishcodes.connectsphere.repository;

import me.manishcodes.connectsphere.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface MessageRepository extends JpaRepository<Message, UUID> {
}
