# Today Plan - Flutter integration contract

## 1. Purpose

- Weekly Roadmap remains the stable learning path.
- Today Plan selects a small, adaptive set of activities for the current day.
- Backend owns ordering, completion, progress and recommendation reasons. Flutter must not
  calculate or write these fields.
- Business date/time uses `Asia/Ho_Chi_Minh`.

## 2. Endpoints

Both endpoints require a STUDENT access token and return `Cache-Control: no-store`.

```http
GET /api/v1/recommendations/today?budgetMinutes=20
GET /api/v1/learner/profile
```

`budgetMinutes` is optional and must be from 5 to 120. If omitted, backend uses
`dailyStudyMinutes` from Goal Survey, then falls back to 15.

## 3. Today Plan response

The payload below is inside the existing `ApiResponse.data` field.

```json
{
  "date": "2026-10-06",
  "timezone": "Asia/Ho_Chi_Minh",
  "revision": 2,
  "generatedAt": "2026-10-06T08:10:00",
  "expiresAt": "2026-10-07T00:00:00",
  "profileVersion": 17,
  "rulesVersion": "2026-10-06.1",
  "budgetMinutes": 20,
  "estimatedMinutes": 18,
  "completedMinutes": 5,
  "progressPercent": 27.78,
  "isCompleted": false,
  "nextRecommendationId": "64-char-sha256-id",
  "emptyReason": null,
  "activities": [
    {
      "recommendationId": "64-char-sha256-id",
      "type": "PRONUNCIATION",
      "title": null,
      "skill": "PRONUNCIATION",
      "estimatedMinutes": 3,
      "target": { "phonemeId": 17 },
      "navigation": { "route": "IPA_PHONEME", "phonemeId": 17 },
      "reasonCode": "WEAK_PHONEME",
      "reasonParams": { "phonemeId": 17, "avgScore": 48 },
      "priority": null,
      "status": "IN_PROGRESS",
      "completedUnits": 1,
      "totalUnits": 3,
      "progressPercent": 33.33,
      "isCompleted": false,
      "completedAt": null
    }
  ]
}
```

### Stable fields and refresh behavior

- `recommendationId` is stable for the same date, type and target. Use it as the Flutter list key.
- `revision` increases only when backend rebuilds the set of pending recommendations.
- Items already `IN_PROGRESS` or `COMPLETED` remain in that day's plan after a rebuild.
- Refresh this endpoint whenever the learner returns from an activity. The API reconciles source
  tables, so correctness does not depend on the asynchronous event worker finishing first.
- Do not add a generic "complete recommendation" call. Completion comes from vocabulary,
  pronunciation, speaking, roadmap or assignment data.

### Empty and completed states

- `activities=[]`, `emptyReason=NO_ELIGIBLE_ACTIVITIES`, `isCompleted=false`: there is currently
  no suitable work; render an empty-state message, not a completion celebration.
- Non-empty activities and `isCompleted=true`: every persisted activity for today is completed.
- `nextRecommendationId` points to the first unfinished activity and is `null` when all are done.

## 4. Activity navigation

Flutter should route from `navigation.route` and treat `target`/`navigation` as typed payloads:

| type | route | target keys |
|---|---|---|
| `VOCABULARY_REVIEW` | `VOCABULARY_REVIEW` | `mode` |
| `ROADMAP_MODULE` | `ROADMAP_MODULE` | `roadmapVersion`, `moduleKey` |
| `PRONUNCIATION` | `IPA_PHONEME` | `phonemeId` |
| `SPEAKING` | `SPEAKING_SESSION` | `scenarioId` |
| `ASSIGNMENT` | `ASSIGNMENT` | `courseId`, `assignmentId` |
| `VOCABULARY_TOPIC` | `VOCABULARY_TOPIC` | `topicId` |

`reasonCode` is a stable localization key. Flutter translates it and may interpolate
`reasonParams`. `priority=P0/P1` means overdue/due within 24 hours; these assignments may make
`estimatedMinutes` exceed `budgetMinutes`. Other recommendations remain inside the budget.

## 5. Recommended Flutter flow

1. Load Today Plan after login/home initialization.
2. Render activities in the returned order; do not re-sort.
3. Open the destination from `navigation`.
4. After an activity finishes or the screen resumes, call Today Plan again.
5. Replace local data only after a successful response; retain the last good response on a
   transient error.
6. Stop any loading indicator when an empty plan is returned; no polling loop is needed.

## 6. Deployment prerequisites

- Flyway migrations `V62`, `V63`, and `V64` must succeed before enabling the endpoints.
- Production may keep `ADAPTIVE_WORKER_POLL_MS=300000`; new events wake the worker after commit,
  while the poll is only a recovery mechanism.
- Optional recommendation environment variables use the defaults documented in
  `application.yaml`; changing weights requires increasing `ADAPTIVE_RECOMMENDATION_RULES_VERSION`.
