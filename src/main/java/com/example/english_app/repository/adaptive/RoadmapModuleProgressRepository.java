package com.example.english_app.repository.adaptive;

import com.example.english_app.entity.adaptive.RoadmapModuleProgress;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface RoadmapModuleProgressRepository extends JpaRepository<RoadmapModuleProgress, Long> {

    List<RoadmapModuleProgress> findByStudentIdAndRoadmapVersionOrderByWeekNumberAscModuleIndexAsc(
            Long studentId, Integer roadmapVersion);

}
