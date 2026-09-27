package com.example.english_app.controller;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

import static org.assertj.core.api.Assertions.assertThat;

class OnboardingAuthorizationContractTest {

    @Test
    void studentControllerIsRestrictedToStudents() {
        PreAuthorize annotation = OnboardingController.class.getAnnotation(PreAuthorize.class);

        assertThat(annotation).isNotNull();
        assertThat(annotation.value()).isEqualTo("hasRole('STUDENT')");
    }

    @Test
    void adminResetControllerIsRestrictedToAdmins() {
        PreAuthorize annotation = AdminOnboardingController.class.getAnnotation(PreAuthorize.class);

        assertThat(annotation).isNotNull();
        assertThat(annotation.value()).isEqualTo("hasRole('ADMIN')");
    }
}
