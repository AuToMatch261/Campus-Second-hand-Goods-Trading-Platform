package com.campus.auth.service;

import com.campus.auth.dto.LoginRequest;
import com.campus.auth.dto.LoginResult;
import com.campus.auth.dto.RegisterRequest;
import com.campus.auth.dto.UserVO;
import com.campus.auth.entity.User;
import com.campus.auth.mapper.UserMapper;
import com.campus.auth.service.impl.AuthServiceImpl;
import com.campus.common.exception.BusinessException;
import com.campus.common.response.ResultCode;
import com.campus.common.security.JwtProperties;
import com.campus.common.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    UserMapper userMapper;

    PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    JwtProperties jwtProperties;
    JwtUtil jwtUtil;

    @InjectMocks
    AuthServiceImpl service;

    @BeforeEach
    void setup() {
        jwtProperties = new JwtProperties();
        jwtProperties.setSecret("test-secret-test-secret-test-secret-1234567890");
        jwtProperties.setExpireMinutes(60);
        jwtUtil = new JwtUtil(jwtProperties);
        service = new AuthServiceImpl(userMapper, passwordEncoder, jwtUtil, jwtProperties);
    }

    @Test
    void register_success() {
        when(userMapper.selectCount(any())).thenReturn(0L);
        when(userMapper.insert(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(42L);
            return 1;
        });

        RegisterRequest req = new RegisterRequest();
        req.setUsername("alice");
        req.setPassword("p@ss1234");
        req.setNickname("Alice");

        UserVO vo = service.register(req);

        assertThat(vo.getId()).isEqualTo(42L);
        assertThat(vo.getUsername()).isEqualTo("alice");
        assertThat(vo.getNickname()).isEqualTo("Alice");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        org.mockito.Mockito.verify(userMapper).insert(captor.capture());
        // 密码必须是 BCrypt 哈希，不能是明文
        assertThat(captor.getValue().getPasswordHash()).isNotEqualTo("p@ss1234");
        assertThat(passwordEncoder.matches("p@ss1234", captor.getValue().getPasswordHash())).isTrue();
    }

    @Test
    void register_duplicate_username() {
        when(userMapper.selectCount(any())).thenReturn(1L);
        RegisterRequest req = new RegisterRequest();
        req.setUsername("alice");
        req.setPassword("p@ss1234");

        assertThatThrownBy(() -> service.register(req))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", ResultCode.USER_ALREADY_EXISTS.getCode());
    }

    @Test
    void login_success_returns_token_and_user() {
        User stored = new User();
        stored.setId(1L);
        stored.setUsername("alice");
        stored.setPasswordHash(passwordEncoder.encode("p@ss1234"));
        stored.setStatus(1);
        stored.setNickname("Alice");
        when(userMapper.selectOne(any())).thenReturn(stored);

        LoginRequest req = new LoginRequest();
        req.setUsername("alice");
        req.setPassword("p@ss1234");

        LoginResult r = service.login(req);

        assertThat(r.getToken()).isNotBlank();
        assertThat(r.getUser().getId()).isEqualTo(1L);
        assertThat(r.getExpireMinutes()).isEqualTo(60);

        // Token 能解出 userId
        assertThat(jwtUtil.getUserId(r.getToken())).isEqualTo(1L);
        assertThat(jwtUtil.getUsername(r.getToken())).isEqualTo("alice");
    }

    @Test
    void login_wrong_password() {
        User stored = new User();
        stored.setId(1L);
        stored.setUsername("alice");
        stored.setPasswordHash(passwordEncoder.encode("correct"));
        stored.setStatus(1);
        when(userMapper.selectOne(any())).thenReturn(stored);

        LoginRequest req = new LoginRequest();
        req.setUsername("alice");
        req.setPassword("wrong");

        assertThatThrownBy(() -> service.login(req))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", ResultCode.USER_PASSWORD_ERROR.getCode());
    }

    @Test
    void login_user_not_found() {
        when(userMapper.selectOne(any())).thenReturn(null);
        LoginRequest req = new LoginRequest();
        req.setUsername("ghost");
        req.setPassword("x");

        assertThatThrownBy(() -> service.login(req))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void login_disabled_account() {
        User stored = new User();
        stored.setId(1L);
        stored.setUsername("alice");
        stored.setPasswordHash(passwordEncoder.encode("p@ss1234"));
        stored.setStatus(0);
        when(userMapper.selectOne(any())).thenReturn(stored);

        LoginRequest req = new LoginRequest();
        req.setUsername("alice");
        req.setPassword("p@ss1234");

        assertThatThrownBy(() -> service.login(req))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", ResultCode.FORBIDDEN.getCode());
    }

    @Test
    void getCurrentUser_not_found() {
        when(userMapper.selectById(99L)).thenReturn(null);
        assertThatThrownBy(() -> service.getCurrentUser(99L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", ResultCode.USER_NOT_FOUND.getCode());
    }
}
