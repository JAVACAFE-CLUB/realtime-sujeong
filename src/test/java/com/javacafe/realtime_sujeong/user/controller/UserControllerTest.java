package com.javacafe.realtime_sujeong.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.javacafe.realtime_sujeong.user.dto.UserCreateRequest;
import com.javacafe.realtime_sujeong.user.dto.UserResponse;
import com.javacafe.realtime_sujeong.user.dto.UserUpdateRequest;
import com.javacafe.realtime_sujeong.user.entity.User;
import com.javacafe.realtime_sujeong.user.service.UserService;
import com.navercorp.fixturemonkey.FixtureMonkey;
import com.navercorp.fixturemonkey.api.introspector.ConstructorPropertiesArbitraryIntrospector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    private FixtureMonkey fixtureMonkey;
    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        fixtureMonkey = FixtureMonkey.builder()
                .objectIntrospector(ConstructorPropertiesArbitraryIntrospector.INSTANCE)
                .build();

        mockMvc = MockMvcBuilders.standaloneSetup(userController).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void 유저_전체_조회_API() throws Exception {
        List<User> users = fixtureMonkey.giveMe(User.class, 3);
        List<UserResponse> responses = users.stream()
                .map(UserResponse::toEntity)
                .toList();
        given(userService.findAllUsers()).willReturn(responses);


        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3));
    }

    @Test
    void 유저_생성_API() throws Exception {
        UserCreateRequest request = fixtureMonkey.giveMeBuilder(UserCreateRequest.class)
                .set("email", "valid@email.com")
                .set("password", "qwer1234!")
                .set("name", "username")
                .sample();

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(userService).createUser(any(UserCreateRequest.class));
    }

    @Test
    void 유저_업데이트_API() throws Exception {
        UserUpdateRequest request = fixtureMonkey.giveMeOne(UserUpdateRequest.class);

        mockMvc.perform(put("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(userService).updateUser(any(UserUpdateRequest.class));
    }

    @Test
    void 유저_삭제_API() throws Exception {
        Long id = 1L;

        mockMvc.perform(delete("/api/users/{id}", id))
                .andExpect(status().isOk());

        verify(userService).deleteUserById(id);
    }
}
