package com.javacafe.realtime_sujeong.user.service;

import com.javacafe.realtime_sujeong.user.dto.UserCreateRequest;
import com.javacafe.realtime_sujeong.user.dto.UserUpdateRequest;
import com.javacafe.realtime_sujeong.user.entity.User;
import com.javacafe.realtime_sujeong.user.repository.UserRepository;

import com.navercorp.fixturemonkey.FixtureMonkey;
import com.navercorp.fixturemonkey.api.introspector.ConstructorPropertiesArbitraryIntrospector;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;


@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private FixtureMonkey fixtureMonkey;

    @BeforeEach
    void setUp() {
        fixtureMonkey = FixtureMonkey.builder()
                .objectIntrospector(ConstructorPropertiesArbitraryIntrospector.INSTANCE)
                .build();
    }

    @Test
    void 유저_전체_조회() {
        // given
        List<User> userList = fixtureMonkey.giveMe(User.class, 3);
        given(userRepository.findAll()).willReturn(userList);

        // when
        var result = userService.findAllUsers();

        // then
        assertThat(result).hasSize(3);
        assertThat(result.get(0).email()).isEqualTo(userList.get(0).getEmail());
    }

    @Test
    void 이메일로_유저_조회_성공() {
        // given
        User user = fixtureMonkey.giveMeOne(User.class);
        given(userRepository.findByEmail(user.getEmail())).willReturn(Optional.of(user));

        // when
        var result = userService.findUserByEmail(user.getEmail());

        // then
        assertThat(result.email()).isEqualTo(user.getEmail());
        assertThat(result.name()).isEqualTo(user.getName());
    }

    @Test
    void 이메일로_유저_조회_실패() {
        // given
        given(userRepository.findByEmail("noone@none.com")).willReturn(Optional.empty());

        // then
        assertThatThrownBy(() -> userService.findUserByEmail("noone@none.com"))
                .isInstanceOf(NoSuchElementException.class); // or custom exception
    }

    @Test
    void 유저_생성_성공() {
        // given
        UserCreateRequest request = fixtureMonkey.giveMeBuilder(UserCreateRequest.class)
                .set("email", "new@email.com")
                .sample();

        given(userRepository.findByEmail(request.getEmail())).willReturn(Optional.empty());
        given(userRepository.save(any(User.class))).willAnswer(inv -> inv.getArgument(0));

        // when
        userService.createUser(request);

        // then
        verify(userRepository).save(any(User.class));
    }

    @Test
    void 유저_생성_실패_중복_이메일() {
        // given
        UserCreateRequest request = fixtureMonkey.giveMeOne(UserCreateRequest.class);
        String duplicateEmail = request.getEmail();  // 랜덤 생성된 이메일

        User user = User.builder()
                .id(1L)
                .email(duplicateEmail)  // request의 이메일과 동일하게 맞춰야 함
                .password("pass")
                .name("dup")
                .build();

        given(userRepository.findByEmail(duplicateEmail)).willReturn(Optional.of(user));

        // when & then
        assertThatThrownBy(() -> userService.createUser(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("이미 존재하는 이메일입니다.");
    }

    @Test
    void 유저_업데이트_성공() {
        // given
        User user = fixtureMonkey.giveMeOne(User.class);
        UserUpdateRequest request = fixtureMonkey.giveMeBuilder(UserUpdateRequest.class)
                .set("email", user.getEmail())
                .sample();

        given(userRepository.findByEmail(request.getEmail())).willReturn(Optional.of(user));

        // when
        userService.updateUser(request);

        // then
        assertThat(user.getPassword()).isEqualTo(request.getPassword());
        assertThat(user.getName()).isEqualTo(request.getName());
    }

    @Test
    void 유저_삭제_성공() {
        // given
        User user = fixtureMonkey.giveMeOne(User.class);
        given(userRepository.findByEmail(user.getEmail())).willReturn(Optional.of(user));

        // when
        userService.deleteUserByEmail(user.getEmail());

        // then
        verify(userRepository).delete(user);
    }

    @Test
    void 유저_삭제_실패_이메일_없음() {
        // given
        given(userRepository.findByEmail("not@found.com")).willReturn(Optional.empty());

        // then
        assertThatThrownBy(() -> userService.deleteUserByEmail("not@found.com"))
                .isInstanceOf(NoSuchElementException.class);
    }
}
