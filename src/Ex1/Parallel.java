package Ex1;

import java.util.ArrayList;
import java.util.List;

public class Parallel {

    @FunctionalInterface
    public interface Task<T> {
        T run(int startIndex, int steps);
    }

    public static <T> List<T> runParallel(Task<T> task, int totalSteps, int numThreads) throws InterruptedException {
        int chunkSize = totalSteps / numThreads;
        int remainder = totalSteps % numThreads;

        List<Thread> threads = new ArrayList<>();
        List<T> results = new ArrayList<>();
        Object lock = new Object();

        int currentStart = 0;

        for (int i = 0; i < numThreads; i++) {
            int steps = chunkSize + (i < remainder ? 1 : 0);
            int threadStart = currentStart;

            Thread t = new Thread(() -> {
                T result = task.run(threadStart, steps);
                synchronized (lock) {
                    results.add(result);
                }
            });

            threads.add(t);
            t.start();

            currentStart += steps;
        }

        for (Thread t : threads) {
            t.join();
        }

        return results;
    }
}
