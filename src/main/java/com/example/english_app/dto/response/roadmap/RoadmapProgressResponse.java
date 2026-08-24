package com.example.english_app.dto.response.roadmap;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoadmapProgressResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    private String cefrLevel;
    private int totalWeeks;
    private int completedWeeks;
    private int currentWeek;
    private int totalModules;
    private int completedModules;
    private double percentCompleted;
    private String nextSuggestedModule;
    private List<RoadmapMilestone> milestones;
}
