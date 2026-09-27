package com.example.english_app.service.user;

import com.example.english_app.dto.request.UserCreateRequest;
import com.example.english_app.entity.enums.AuthProvider;
import com.example.english_app.entity.enums.Role;
import com.example.english_app.entity.user.User;
import com.example.english_app.repository.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @InjectMocks private AdminUserService service;

    @Test
    void adminProvisionedTeacherIsLocalAndEmailIsNormalized() {
        UserCreateRequest request = new UserCreateRequest();
        request.setEmail(" Teacher@Example.COM ");
        request.setPassword("Password1!");
        request.setFullName("Teacher");
        request.setRole(Role.TEACHER);

        when(passwordEncoder.encode(request.getPassword())).thenReturn("encoded");
        when(userRepository.save(org.mockito.ArgumentMatchers.any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.create(request);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).existsByEmail("teacher@example.com");
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getEmail()).isEqualTo("teacher@example.com");
        assertThat(captor.getValue().getProvider()).isEqualTo(AuthProvider.LOCAL);
        assertThat(captor.getValue().getRole()).isEqualTo(Role.TEACHER);
        assertThat(captor.getValue().getOnboardingCompleted()).isFalse();
    }
}
