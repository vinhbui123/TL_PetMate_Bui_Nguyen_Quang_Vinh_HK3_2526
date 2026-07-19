package com.petmate.server.repository;

import com.petmate.server.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;
import com.petmate.server.enums.RoleType;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByProviderId(String providerId);
    List<User> findByRole(RoleType role);
    List<User> findAllByOrderByIdDesc();
}
