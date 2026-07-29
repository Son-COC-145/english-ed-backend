package com.example.english_app.repository.classroom;

import com.example.english_app.entity.classroom.TeachingMaterial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TeachingMaterialRepository extends JpaRepository<TeachingMaterial, Long> {
    List<TeachingMaterial> findAllByCourseId(Long courseId);
    List<TeachingMaterial> findAllByTeacherId(Long teacherId);
}
