package com.javacafe.realtime_sujeong.user.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.javacafe.realtime_sujeong.user.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);
}
