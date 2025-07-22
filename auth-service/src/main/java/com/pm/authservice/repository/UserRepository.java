/**
 Repository interface for accessing User entities from the database.

 Provides CRUD operations via JpaRepository and
 a custom method to find a user by their email address.
 */

package com.pm.authservice.repository;

import com.pm.authservice.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
}
