package dev.by1337.bc.util;

@FunctionalInterface
public interface ThrowingRunnable {
    void run() throws Throwable;
}
