package Ex2;

import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadLocalRandom;

public class MonteCarloPiProducerConsumer {

    static class Point {
        final double x, y;
        Point(double x, double y) {
            this.x = x;
            this.y = y;
        }
    }

    public static void main(String[] args) throws InterruptedException {
        int totalSamples = 10_000_000;
        int queueCapacity = 10_000; // buffer size

        BlockingQueue<Point> queue = new ArrayBlockingQueue<>(queueCapacity);

        // Shared flag to signal producers are done
        final int producers = 2;
        final boolean[] finished = new boolean[producers];

        long start = System.nanoTime();

        // Producer threads
        for (int p = 0; p < producers; p++) {
            final int index = p;
            new Thread(() -> {
                int samplesPerProducer = totalSamples / producers;
                for (int i = 0; i < samplesPerProducer; i++) {
                    double x = ThreadLocalRandom.current().nextDouble(-1.0, 1.0);
                    double y = ThreadLocalRandom.current().nextDouble(-1.0, 1.0);
                    try {
                        queue.put(new Point(x, y)); // blocks if full
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
                finished[index] = true;
            }, "Producer-" + (p + 1)).start();
        }

        // Consumer thread
        final int samplesToConsume = totalSamples;
        Thread consumer = new Thread(() -> {
            int inside = 0;
            int received = 0;

            try {
                while (received < samplesToConsume) {
                    Point p = queue.take(); // blocks if empty
                    double dist = p.x * p.x + p.y * p.y;
                    if (dist <= 1.0) inside++;
                    received++;
                }

                double piEstimate = 4.0 * inside / samplesToConsume;
                long end = System.nanoTime();
                double seconds = (end - start) / 1_000_000_000.0;

                System.out.printf("Estimativa de π após %,d amostras: %.6f%n",
                        samplesToConsume, piEstimate);
                System.out.printf("Tempo de execução: %.3f segundos%n", seconds);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "Consumer");

        consumer.start();
        consumer.join(); // wait for consumer to finish
    }
}

