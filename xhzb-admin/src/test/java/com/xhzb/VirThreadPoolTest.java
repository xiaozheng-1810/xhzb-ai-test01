package com.xhzb;

import org.apache.tomcat.util.threads.VirtualThreadExecutor;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class VirThreadPoolTest {

    public static int count = 0;

    public static void main(String[] args) {
        //1.创建虚拟线程池
        ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor();


        for (int i = 0; i < 1000000; i++) {
            executorService.submit(new Runnable() {
                @Override
                public void run() {
                    count++;
                    int num = count;
                    while (true) {
                        System.out.println("我是任务" + num);
                    }
                }
            });
        }

        try {
            Thread.sleep(60 * 1000 * 5);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

    }
}
