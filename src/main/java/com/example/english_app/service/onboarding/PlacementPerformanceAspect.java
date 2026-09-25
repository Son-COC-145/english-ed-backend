package com.example.english_app.service.onboarding;

import com.example.english_app.dto.response.PlacementQuestionResponse;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/** Low-cardinality latency instrumentation for the placement hot paths. */
@Slf4j
@Aspect
@Order(10)
@Component
@RequiredArgsConstructor
public class PlacementPerformanceAspect {

    private static final long ANSWER_SLO_MS = 300;
    private final MeterRegistry meterRegistry;

    @Around("execution(* com.example.english_app.service.onboarding.PlacementTestService.submitAnswer(..))")
    public Object measureAnswerSubmit(ProceedingJoinPoint joinPoint) throws Throwable {
        long started = System.nanoTime();
        String outcome = "success";
        boolean completion = false;
        try {
            Object result = joinPoint.proceed();
            completion = result instanceof PlacementQuestionResponse response
                    && response.isTestCompleted();
            return result;
        } catch (Throwable throwable) {
            outcome = "error";
            throw throwable;
        } finally {
            long elapsed = System.nanoTime() - started;
            Timer.builder("onboarding.placement.answer.duration")
                    .tag("outcome", outcome)
                    .tag("completion", Boolean.toString(completion))
                    .publishPercentileHistogram()
                    .register(meterRegistry)
                    .record(elapsed, TimeUnit.NANOSECONDS);
            long elapsedMs = TimeUnit.NANOSECONDS.toMillis(elapsed);
            if (elapsedMs > ANSWER_SLO_MS) {
                log.warn("Placement answer SLO exceeded: elapsedMs={}, completion={}",
                        elapsedMs, completion);
            }
        }
    }

    @Around("execution(* com.example.english_app.service.onboarding.PronunciationService.submitPronunciationWithProgression(..))")
    public Object measurePronunciationSubmit(ProceedingJoinPoint joinPoint) throws Throwable {
        Timer.Sample sample = Timer.start(meterRegistry);
        String outcome = "success";
        try {
            return joinPoint.proceed();
        } catch (Throwable throwable) {
            outcome = "error";
            throw throwable;
        } finally {
            sample.stop(Timer.builder("onboarding.placement.pronunciation.duration")
                    .tag("outcome", outcome)
                    .publishPercentileHistogram()
                    .register(meterRegistry));
        }
    }
}
