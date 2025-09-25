package com.forA.chatbot.auth.repository;

import com.forA.chatbot.user.domain.User;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

  Optional<User> findByAppleUniqueId(String appleUniqueId);

  Optional<User> findByEmail(String email);

  @Query(
      "SELECT u FROM User u WHERE u.isDeleted = true AND u.deletedAt IS NOT NULL AND u.deletedAt <"
          + " :cutoffDate")
  List<User> findUsersToBeDeleted(@Param("cutoffDate") LocalDateTime cutoffDate);
}
