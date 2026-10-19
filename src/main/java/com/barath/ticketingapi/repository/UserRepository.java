package com.barath.ticketingapi.repository;

import com.barath.ticketingapi.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    //to check if an account already exists
    boolean existsByEmail(String email);

    Optional<User> findByEmail(String email);

}
