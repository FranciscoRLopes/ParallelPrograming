import Ex1.Parallel;
import java.util.List;

public class MonteCarloPiLib {

    public static int estimatePiChunk(int start, int steps) {
        int inside = 0;
        for (int i = 0; i < steps; i++) {
            double x = Math.random() * 2 - 1;
            double y = Math.random() * 2 - 1;
            if (x*x + y*y <= 1) inside++;
        }
        return inside;
    }

    public static void main(String[] args) throws InterruptedException {
        int samples = 100_000_000;
        int numThreads = 8;

        List<Integer> results = Parallel.runParallel(MonteCarloPiLib::estimatePiChunk, samples, numThreads);

        long start = System.nanoTime();

        int totalInside = results.stream().mapToInt(Integer::intValue).sum();
        double pi = 4.0 * totalInside / samples;


        long end = System.nanoTime();
        double durationInSeconds = (end - start) / 1_000_000_000.0;

        System.out.println("Estimativa de π: " + pi);
        System.out.printf("Tempo de execução: %.3f segundos%n", durationInSeconds);
    }
}
