package com.example.english_app.repository;

import com.example.english_app.entity.vocabulary.Topic;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;


public interface TopicRepository extends JpaRepository<Topic, Short> {
    List<Topic> findByNameEnContainingIgnoreCase(String nameEn);

    List<Topic> findByNameViContainingIgnoreCase(String nameVi);

    boolean existsByNameEn(String nameEn);

    boolean existsByNameVi(String nameVi);
}
