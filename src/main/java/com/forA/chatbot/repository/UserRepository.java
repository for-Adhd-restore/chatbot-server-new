package com.forA.chatbot.repository;

import com.forA.chatbot.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByAppleUniqueId(String appleUniqueId);
}
