package com.javacafe.realtime_sujeong.user.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.javacafe.realtime_sujeong.user.dto.UserCreateRequest;
import com.javacafe.realtime_sujeong.user.dto.UserResponse;
import com.javacafe.realtime_sujeong.user.dto.UserUpdateRequest;
import com.javacafe.realtime_sujeong.user.entity.User;
import com.javacafe.realtime_sujeong.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;

    public List<UserResponse> findAllUsers() {
        List<User> users = userRepository.findAll();
        List<UserResponse> userResponseList = new ArrayList<>();
        users.forEach(user -> userResponseList.add(UserResponse.toEntity(user)));
        return userResponseList;
    }

    public UserResponse findUserById(long id) {
        User user = userRepository.findById(id).orElseThrow();
        return UserResponse.toEntity(user);
    }

    @Transactional
    public void createUser(UserCreateRequest userCreateRequest) {
        checkEmail(userCreateRequest.getEmail());
        userRepository.save(userCreateRequest.toEntity());
    }

    @Transactional
    public void updateUser(UserUpdateRequest userUpdateRequest) {
        User user = userRepository.findById(userUpdateRequest.getId()).orElseThrow();
        user.update(userUpdateRequest);
    }

    @Transactional
    public void deleteUserById(long id) {
        User user = userRepository.findById(id).orElseThrow();
        userRepository.delete(user);
    }

    /**
     * email 중복 확인
     * 이미 존재하는 email인 경우 예외 발생
     */
    private void checkEmail(String email) {
        if (userRepository.findByEmail(email).isPresent()) {
            throw new RuntimeException("이미 존재하는 이메일입니다.");
        }
    }
}
