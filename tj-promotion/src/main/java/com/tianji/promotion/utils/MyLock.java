package com.tianji.promotion.utils;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface MyLock {
    String name();

    long waitTime() default 1;

    long leaseTime() default -1;

    TimeUnit unit() default TimeUnit.SECONDS;

    //锁的类型 默认为可重入锁
    MyLockType lockType() default MyLockType.RE_ENTRANT_LOCK;

    //代表获取锁的失败策略 默认为重试超时后抛出异常
    MyLockStrategy lockStrategy() default MyLockStrategy.FAIL_AFTER_RETRY_TIMEOUT;
}