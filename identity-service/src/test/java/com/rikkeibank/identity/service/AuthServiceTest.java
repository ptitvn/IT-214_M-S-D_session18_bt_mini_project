package com.rikkeibank.identity.service;

import com.rikkeibank.identity.dto.AuthDtos.LoginRequest;
import com.rikkeibank.identity.entity.Role;
import com.rikkeibank.identity.entity.User;
import com.rikkeibank.identity.repository.UserRepository;
import com.rikkeibank.identity.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private StringRedisTemplate redisTemplate;

    @InjectMocks
    private AuthService authService;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder().id(1L).username("customer1").password("encoded")
                .role(Role.CUSTOMER).referenceId(1L).enabled(true).build();
        ReflectionTestUtils.setField(authService, "refreshTokenExpirationMs", 2592000000L);
    }

    @Test
    void login_shouldThrow_whenPasswordDoesNotMatch() {
        when(userRepository.findByUsername("customer1")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongpass", "encoded")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("customer1", "wrongpass")))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void login_shouldThrow_whenUserDisabled() {
        user.setEnabled(false);
        when(userRepository.findByUsername("customer1")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login(new LoginRequest("customer1", "any")))
                .isInstanceOf(BadCredentialsException.class);
    }
}
