package com.akgeneralstore.repository;

import com.akgeneralstore.entity.User;
import com.akgeneralstore.enums.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByPhone(String phone);
    long countByRole(UserRole role);
    List<User> findByRole(UserRole role);
}
