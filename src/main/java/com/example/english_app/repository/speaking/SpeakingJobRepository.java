package com.example.english_app.repository.speaking;

import java.util.Optional;
import com.example.english_app.entity.speaking.SpeakingJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpeakingJobRepository extends JpaRepository<SpeakingJob, Long> {
    @Modifying(flushAutomatically = true)
    @Query(value = "insert into speaking_jobs(session_id, turn_id, kind) values (:sessionId,:turnId,:kind) on conflict do nothing", nativeQuery = true)
    void enqueue(@Param("sessionId") Long sessionId, @Param("turnId") Long turnId, @Param("kind") String kind);

    @Modifying(flushAutomatically = true)
    @Query(value = """
            update speaking_jobs set status='PENDING', max_attempts=attempts+3,
            available_at=CURRENT_TIMESTAMP, updated_at=CURRENT_TIMESTAMP,
            finished_at=null, error_code=null, error_message=null
            where session_id=:sessionId and status='FAILED'
              and (cast(:turnId as bigint) is null or turn_id=:turnId or kind='SESSION_EVALUATION')
            """, nativeQuery = true)
    void resetFailed(@Param("sessionId") Long sessionId, @Param("turnId") Long turnId);

    @Query(value = """
            select j.* from speaking_jobs j
            where ((j.status='PENDING' and j.available_at <= CURRENT_TIMESTAMP)
                or (j.status='RUNNING' and j.lease_expires_at <= CURRENT_TIMESTAMP))
            and (j.kind <> 'SESSION_EVALUATION'
                or exists (select 1 from speaking_turns t where t.session_id=j.session_id
                    and (t.status='FAILED' or t.evaluation_status='FAILED'))
                or not exists (select 1 from speaking_turns t where t.session_id=j.session_id
                    and (t.status <> 'COMPLETED' or (t.speaker='STUDENT' and t.evaluation_status <> 'COMPLETED'))))
            order by j.available_at, j.id limit 1 for update of j skip locked
            """, nativeQuery = true)
    Optional<SpeakingJob> findEligibleForUpdate();

    @Modifying(flushAutomatically = true)
    @Query(value = """
            update speaking_job_attempts set status='TIMED_OUT', finished_at=CURRENT_TIMESTAMP,
            error_code='LEASE_EXPIRED', error_message='Worker lease expired', retryable=true
            where job_id=:id and status='RUNNING'
            """, nativeQuery = true)
    void expireAttempts(@Param("id") long id);

    @Modifying(flushAutomatically = true)
    @Query(value = """
            update speaking_jobs set status='RUNNING', attempts=attempts+1, lease_token=:token,
            lease_expires_at=CURRENT_TIMESTAMP + interval '5 minutes', started_at=CURRENT_TIMESTAMP,
            updated_at=CURRENT_TIMESTAMP, finished_at=null where id=:id
            """, nativeQuery = true)
    void acquireLease(@Param("id") long id, @Param("token") String token);

    @Modifying(flushAutomatically = true)
    @Query(value = "insert into speaking_job_attempts(job_id, attempt_no, status, lease_token) values (:id,:attempt,'RUNNING',:token)", nativeQuery = true)
    void recordAttempt(@Param("id") long id, @Param("attempt") int attempt, @Param("token") String token);

    @Query(value = """
            select id from speaking_jobs where id=:id and lease_token=:token and status='RUNNING'
            and lease_expires_at > CURRENT_TIMESTAMP for update
            """, nativeQuery = true)
    Optional<Long> findCurrentForUpdate(@Param("id") long id, @Param("token") String token);

    @Query(value = "select max_attempts from speaking_jobs where id=:id", nativeQuery = true)
    int maxAttempts(@Param("id") long id);

    @Modifying(flushAutomatically = true)
    @Query(value = """
            update speaking_jobs set status='FAILED', error_code='TURN_PROCESSING_FAILED',
            error_message='A required turn failed', finished_at=CURRENT_TIMESTAMP,
            updated_at=CURRENT_TIMESTAMP, lease_expires_at=null where id=:id
            """, nativeQuery = true)
    void blockReport(@Param("id") long id);

    @Modifying(flushAutomatically = true)
    @Query(value = """
            update speaking_jobs set status='COMPLETED', error_code=null, error_message=null,
            finished_at=CURRENT_TIMESTAMP, updated_at=CURRENT_TIMESTAMP, lease_expires_at=null where id=:id
            """, nativeQuery = true)
    void complete(@Param("id") long id);

    @Modifying(flushAutomatically = true)
    @Query(value = """
            update speaking_jobs set status='PENDING', max_attempts=max_attempts+1, lease_expires_at=null,
            updated_at=CURRENT_TIMESTAMP, available_at=CURRENT_TIMESTAMP + interval '2 seconds' where id=:id
            """, nativeQuery = true)
    void defer(@Param("id") long id);

    @Modifying(flushAutomatically = true)
    @Query(value = """
            update speaking_jobs set status=:status, error_code=:code, error_message=:message,
            available_at=CURRENT_TIMESTAMP + (:delay * interval '1 second'), lease_expires_at=null,
            updated_at=CURRENT_TIMESTAMP, finished_at=case when :terminal then CURRENT_TIMESTAMP else null end where id=:id
            """, nativeQuery = true)
    void updateFailure(@Param("id") long id, @Param("status") String status, @Param("terminal") boolean terminal,
                       @Param("code") String code, @Param("message") String message, @Param("delay") int delaySeconds);

    @Modifying(flushAutomatically = true)
    @Query(value = """
            update speaking_job_attempts set status=:status, finished_at=CURRENT_TIMESTAMP,
            error_code=:code, error_message=:message, retryable=:retryable
            where job_id=:id and lease_token=:token and status='RUNNING'
            """, nativeQuery = true)
    void finishAttempt(@Param("id") long id, @Param("token") String token, @Param("status") String status,
                       @Param("code") String code, @Param("message") String message, @Param("retryable") boolean retryable);

}
