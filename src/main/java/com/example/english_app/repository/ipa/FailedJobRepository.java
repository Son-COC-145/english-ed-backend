package com.example.english_app.repository.ipa;

import com.example.english_app.entity.ipa.FailedJob;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FailedJobRepository extends JpaRepository<FailedJob, Long> {
    List<FailedJob> findByStatus(String status);
}
