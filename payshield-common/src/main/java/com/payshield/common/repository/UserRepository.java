package com.payshield.common.repository;

import com.payshield.common.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * MySQL Repository - User Master Data
 */
@Repository
public interface UserRepository extends JpaRepository<User, String> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    @Query("SELECT u FROM User u WHERE u.id = :userId AND u.status = 'ACTIVE'")
    Optional<User> findActiveUserById(String userId);

    @Query("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END FROM User u WHERE u.id = :userId AND u.status = 'ACTIVE'")
    boolean isUserActiveById(String userId);
}
