package Ex1;

import java.util.Random;

public class MonteCarloPi {

    public static double estimatePi(int numSamples) {
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

        return 4.0 * insideCircle / numSamples;
    }

    public static void main(String[] args) {
        int samples = 100_000_000;

        long start = System.nanoTime();
        double piEstimate = estimatePi(samples);
        long end = System.nanoTime();

        long duration = end - start;
        double durationInSeconds = duration / 1_000_000_000.0;

        System.out.printf("Estimativa de π após %,d amostras: %.6f%n", samples, piEstimate);
        System.out.printf("Tempo de execução: %.3f segundos%n", durationInSeconds);
    }
}