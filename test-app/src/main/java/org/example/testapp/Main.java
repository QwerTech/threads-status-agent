package org.example.testapp;

import java.time.Duration;
import java.util.Random;

public class Main {

    public static final Duration THREAD_BUSY_INTERVAL = Duration.ofMillis(100);
    public static final Duration THREAD_SLEEP_INTERVAL = Duration.ofSeconds(1);

    public static void main(String[] args) {
        Thread swithingThread = new Thread(Main::switchThreadState, "SwitchThreadState");
        swithingThread.start();
    }

    private static void switchThreadState() {
        while (true) {
            sleep(THREAD_SLEEP_INTERVAL);
            long start = System.nanoTime();
            while (System.nanoTime() - start < THREAD_BUSY_INTERVAL.toNanos()) {
                @SuppressWarnings("unused")
                double value = Math.sqrt(new Random().nextDouble());
            }
        }
    }

    private static void infinite(Runnable action) {
        while (!Thread.currentThread().isInterrupted()) {
            action.run();
        }
    }

    private static void sleep(Duration interval) {
        try {
            Thread.sleep(interval.toMillis());
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unused")
    private static void printState(Thread thread) {
        infinite(() -> {
            System.out.println(thread.getState());
            sleep(Duration.ofSeconds(1));
        });
    }
}
