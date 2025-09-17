package Ex1;

import java.util.ArrayList;
import java.util.List;

public class TrapezoidParallel {

    @FunctionalInterface
    interface Function {
        double apply(double x);
    }

    public static double trapezoidPartial(Function f, double start, double resolution, int steps) {
        double total = 0.0;
        for (int i = 0; i < steps; i++) {
            double x0 = start + i * resolution;
            double x1 = x0 + resolution;
            total += 0.5 * (f.apply(x0) + f.apply(x1)) * resolution;
        }
        return total;
    }

    public static double trapezoidIntegralParallel(Function f, double a, double b, double resolution, int numThreads) throws InterruptedException {
        int n = (int) ((b - a) / resolution);
        int chunkSize = n / numThreads;
        int remainder = n % numThreads;

        List<Thread> threads = new ArrayList<>();
        List<Double> results = new ArrayList<>();
        Object lock = new Object();

        double currentStart = a;

        for (int i = 0; i < numThreads; i++) {
            int steps = chunkSize + (i < remainder ? 1 : 0); // distribuir o resto
            double threadStart = currentStart;

            Thread t = new Thread(() -> {
                double partial = trapezoidPartial(f, threadStart, resolution, steps);
                synchronized (lock) {
                    results.add(partial);
                }
            });

            threads.add(t);
            t.start();

            currentStart += steps * resolution;
        }

        for (Thread t : threads) {
            t.join();
        }

        // somar resultados parciais
        double total = 0.0;
        for (double v : results) {
            total += v;
        }

        return total;
    }

    public static void main(String[] args) throws InterruptedException {
        Function f = (x) -> x * (x - 1);

        long start = System.nanoTime();

        double result = trapezoidIntegralParallel(f, 0.0, 1.0, 1e-7, 2);
        long end = System.nanoTime();

        double durationInSeconds = (end - start) / 1_000_000_00.0;

        System.out.println("Integral aproximada: " + result);
        System.out.printf("Tempo de execução: %.3f milisegundos%n", durationInSeconds);
    }
}
