package com.example.english_app.repository.adaptive;

import com.example.english_app.entity.adaptive.LearnerSkillState;
import com.example.english_app.entity.enums.LearnerSkill;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LearnerSkillStateRepository extends JpaRepository<LearnerSkillState, Long> {

    List<LearnerSkillState> findByStudentIdOrderBySkillAsc(Long studentId);

    Optional<LearnerSkillState> findByStudentIdAndSkill(Long studentId, LearnerSkill skill);
}
