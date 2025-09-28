package Ex2;

import java.util.Random;
import java.util.stream.IntStream;
import java.util.concurrent.ThreadLocalRandom;

public class MonteCarlo {
    public static void main(String[] args) {
        int samples = 100_000_000;

        long start = System.nanoTime();

        Random rand = new Random();

        long insideCircle = IntStream.range(0, samples)
                .parallel()
                .mapToLong(i -> {
                    double x = ThreadLocalRandom.current().nextDouble(-1.0, 1.0);
                    double y = ThreadLocalRandom.current().nextDouble(-1.0, 1.0);
                    // Super importante usar o ThreadLocalRandom
                    // O que acontece é que se for utilizado o Random normal, para nao acontecer um deadlock, o random fica com o lock, para
                    // o programa nao parar. Desta forma cada thread faz o seu próprio random, resultando numa
                    // numa execução 100x mais rápida para a mesma sample
                    return ((x * x) + (y * y) <= 1.0) ? 1 : 0;
                })
                .sum();

        double result = 4.0 * insideCircle / samples;

        long end = System.nanoTime();
        double durationInSeconds = (end - start) / 1_000_000_000.0;

        System.out.printf("Estimativa de π após %,d amostras: %.6f%n", samples, result);
        System.out.printf("Tempo de execução: %.3f segundos%n", durationInSeconds);
    }
}
