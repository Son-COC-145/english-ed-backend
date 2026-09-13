package com.example.english_app.annotation;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimitedAi {

    String description() default "Check rate limit for AI usage";

    boolean idempotent() default false;
}
