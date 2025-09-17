package Ex1;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MonteCarloPiParallel {
    public static int estimatePi(int numSamples) {
        Random rand = new Random();
        int insideCircle = 0;

        for (int i = 0; i < numSamples; i++) {
            double x = rand.nextDouble() * 2 - 1; // [-1, 1]
            double y = rand.nextDouble() * 2 - 1; // [-1, 1]

            double distance = (x * x) + (y * y);

            if (distance <= 1.0) {
                insideCircle++;
            }
        }

        return insideCircle;
    }

    public static void main(String[] args) throws InterruptedException {
        int samples = 100_000_000;
        int numThreads = 14;
        int chunksize = samples / numThreads;

        long start = System.nanoTime();

        List<Thread> threads = new ArrayList<>();
        List<Integer> results = new ArrayList<>();

        Object lock = new Object();

        for (int i = 0; i < numThreads; i++) {
            Thread t = new Thread(() -> {
                int inside = estimatePi(chunksize);
                synchronized (lock) {
                    results.add(inside);
                }
            });
            threads.add(t);
            t.start();
        }

        // esperar todas as threads terminarem
        for (Thread t : threads) {
            t.join();
        }

        // somar os resultados
        int total_inside = 0;
        for (int value : results) {
            total_inside += value;
        }

        double result = 4.0 * total_inside / samples;

        long end = System.nanoTime();
        double durationInSeconds = (end - start) / 1_000_000_000.0;

        System.out.printf("Estimativa de π após %,d amostras: %.6f%n", samples, result);
        System.out.printf("Tempo de execução: %.3f segundos%n", durationInSeconds);
    }
}
