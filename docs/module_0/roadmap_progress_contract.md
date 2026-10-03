# Weekly Roadmap Progress Contract

## 1. Boundary

- **Roadmap** is the stable learning direction, grouped by week.
- **Today Plan** is the adaptive daily allocation and will be exposed separately at
  `GET /api/v1/recommendations/today`.
- Roadmap never persists `dayNumber` or splits content using `dailyStudyMinutes`.
- Flutter must treat `moduleKey` as an opaque identifier. Never derive navigation from
  a title or parse the key format.

## 2. Read APIs

- `GET /api/v1/onboarding/roadmap`: returns the stored weekly content snapshot, enriched
  with current module progress.
- `GET /api/v1/onboarding/roadmap/progress`: returns the progress summary and the same
  `week -> module` hierarchy.

Important summary fields:

| Field | Meaning |
|---|---|
| `roadmapVersion` | Per-student generation version |
| `schemaVersion` | Stored JSON contract version |
| `currentCefrLevel`, `targetCefrLevel` | Roadmap start and target levels |
| `status`, `isCompleted` | Overall server-owned state |
| `currentWeek` | First accessible incomplete week; `null` when complete |
| `currentModuleKey` | Preferred incomplete module in the current week |
| `completedWeeks`, `totalWeeks` | Weekly completion counts |
| `completedModules`, `totalModules` | Module completion counts |
| `percentCompleted` | `sum(doneCount) / sum(totalCount) * 100` |

`nextSuggestedModule` is deprecated and temporarily contains a display title. New clients
must use `currentModuleKey`.

## 3. Week and module state

Both weeks and modules expose:

- `status`: `LOCKED`, `AVAILABLE`, `IN_PROGRESS`, or `COMPLETED`.
- `isCompleted`: completion owned by the backend.
- `isAccessible`: whether Flutter may open the item.
- `progressPercent`.
- `unlockCondition`: present only while locked.

Modules additionally expose `doneCount`, `totalCount`, and `completedAt`.

Unlock policy:

- Week 1 is accessible.
- Week N becomes accessible only after week N-1 is complete.
- Every module in the current accessible week may be opened. Today Plan decides which
  module should be recommended first.
- A locked week returns:

```json
{
  "reasonCode": "PREVIOUS_WEEK_REQUIRED",
  "prerequisiteWeek": 1
}
```

Flutter must not calculate or override these states locally.

## 4. Completion sources

| Module type | Completed item source |
|---|---|
| `VOCABULARY` | Vocabulary ID has `lastPracticedAt` |
| `SPEAKING` | Scenario has a completed speaking session |
| `IPA_PRONUNCIATION` | Phoneme has a pronunciation practice log |

A module is complete only when every ID in its immutable `contentItemIds` snapshot has
been completed. New roadmaps containing an empty module, duplicate key, unsupported type,
or invalid snapshot are rejected and never transition to `READY`.

## 5. Event consistency

- `RoadmapJobService.markReady` writes `ROADMAP_GENERATED` in the same transaction as the
  ready roadmap.
- `RoadmapProgressConsumer` recalculates progress for roadmap generation, vocabulary,
  pronunciation, and speaking events.
- Completion events use stable source references, so retries cannot create duplicates.
- Read APIs still recalculate from source tables. This is the correctness fallback when a
  worker is delayed or an activity publisher has not been deployed yet.

## 6. Flutter rules

1. After an activity completes, refresh roadmap progress when returning to the roadmap.
2. Navigate using `currentModuleKey` and the module `type`/`topicId` fields.
3. Display the backend `status`, progress, and unlock reason directly.
4. Do not manufacture daily roadmap nodes. Render Today Plan from its own API when that
   feature becomes available.
5. Keep the last successful response if refresh fails and allow a scoped retry.

## 7. Production worker

Learning events wake the worker immediately after a successful transaction. Scheduled
polling is crash/retry recovery. A resource-conscious production baseline is:

```text
ADAPTIVE_WORKER_POLL_MS=300000
ADAPTIVE_WORKER_BATCH_SIZE=50
ADAPTIVE_WORKER_MAX_ATTEMPTS=5
ADAPTIVE_WORKER_STUCK_AFTER_MS=600000
ADAPTIVE_WORKER_MAX_BACKOFF_SECONDS=300
```

The existing `V59__roadmap_module_progress.sql` is sufficient for the weekly contract.
`V60` and `V61` belong to Learning Event infrastructure.
