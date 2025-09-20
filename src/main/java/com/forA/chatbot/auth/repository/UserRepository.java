package com.forA.chatbot.auth.repository;

import com.forA.chatbot.user.User;
import jakarta.validation.constraints.NotBlank;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

  Optional<User> findByAppleUniqueId(String appleUniqueId);

  Optional<User> findByEmail(String email);
}
