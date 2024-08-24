package com.itheima;

import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ExecutorTest {
    public static void main(String[] args) {
        //创建线程池
//        Executors.newFixedThreadPool() //创建固定线程数的线程池
//        Executors.newSingleThreadExecutor() //创建单线程的线程池
//        Executors.newCachedThreadPool() //缓存线程池
//        Executors.newScheduledThreadPool()  //创建可以延迟执行线程池
        ThreadPoolExecutor poolExecutor = new ThreadPoolExecutor(3,
                5,
                60,
                TimeUnit.SECONDS, new LinkedBlockingDeque<>());
        //1.建议1：如果任务是属于cpu运行型任务，推荐核心线程为cpu的核数
        //2.建议2：如果任务是io型  推荐核心线程为cpu核数的2倍

        poolExecutor.submit(new Runnable() {
            @Override
            public void run() {
                //任务
            }
        });
    }

}
