package com.example.english_app.service.user;

import com.example.english_app.entity.enums.AuthProvider;
import com.example.english_app.entity.enums.Role;
import com.example.english_app.entity.user.User;
import com.example.english_app.repository.user.UserRepository;
import com.example.english_app.service.storage.StoredFileStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountDataPurgeServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private JdbcTemplate jdbcTemplate;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private StoredFileStore storedFileStore;

    @InjectMocks
    private AccountDataPurgeService service;

    @Test
    void purge_anonymizesStudentAndMarksDeletionComplete() {
        User user = User.builder()
                .id(42L)
                .email("student@example.com")
                .password("old-hash")
                .phone("0900000000")
                .fullName("Student Name")
                .avatarUrl("https://example.com/avatar.png")
                .provider(AuthProvider.GOOGLE)
                .providerId("google-id")
                .role(Role.STUDENT)
                .isActive(false)
                .onboardingCompleted(true)
                .build();
        when(userRepository.findByIdForUpdate(42L)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode(anyString())).thenReturn("irreversible-random-hash");
        when(jdbcTemplate.queryForList(anyString(), eq(String.class), eq(42L), eq(42L), eq(42L)))
                .thenReturn(new ArrayList<>());

        service.purge(42L);

        assertThat(user.getEmail()).startsWith("deleted+").endsWith("@deleted.invalid");
        assertThat(user.getPassword()).isEqualTo("irreversible-random-hash");
        assertThat(user.getPhone()).isNull();
        assertThat(user.getFullName()).isEqualTo("Deleted User");
        assertThat(user.getAvatarUrl()).isNull();
        assertThat(user.getProvider()).isEqualTo(AuthProvider.LOCAL);
        assertThat(user.getProviderId()).isNull();
        assertThat(user.getOnboardingCompleted()).isFalse();
        assertThat(user.getDeletedAt()).isNotNull();
    }

    @Test
    void purge_isIdempotentForCompletedTombstone() {
        User user = User.builder()
                .id(42L)
                .email("deleted@example.invalid")
                .fullName("Deleted User")
                .role(Role.STUDENT)
                .isActive(false)
                .deletedAt(LocalDateTime.now())
                .build();
        when(userRepository.findByIdForUpdate(42L)).thenReturn(Optional.of(user));

        service.purge(42L);

        verify(passwordEncoder, never()).encode(anyString());
    }
}
