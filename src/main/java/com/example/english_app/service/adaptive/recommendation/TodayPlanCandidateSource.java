package com.example.english_app.service.adaptive.recommendation;

import java.util.List;

public interface TodayPlanCandidateSource {
    List<TodayPlanCandidate> collect(TodayPlanContext context);
}
