package Master.Worker;

public class Main {
    public static int estimatePi(int numSamples) {
        java.util.Random rand = new java.util.Random();
        int insideCircle = 0;
        for (int i = 0; i < numSamples; i++) {
            double x = rand.nextDouble() * 2 - 1;
            double y = rand.nextDouble() * 2 - 1;
            if ((x * x + y * y) <= 1.0) insideCircle++;
        }
        return insideCircle;
    }

    public static void main(String[] args) throws InterruptedException {
        int samples = 100_000_000;
        int numThreads = 14;
        int chunkSize = samples / numThreads;

        Master master = new Master(numThreads);

        long start = System.nanoTime();

        // Enviar tarefas
        for (int i = 0; i < numThreads; i++) {
            master.submitTask(new Task(chunkSize));
        }

        // Coletar resultados
        int totalInside = 0;
        for (int i = 0; i < numThreads; i++) {
            totalInside += master.getResult();
        }

        master.shutdown();

        double piEstimate = 4.0 * totalInside / samples;
        long end = System.nanoTime();

        double duration = (end - start) / 1_000_000_000.0;
        System.out.printf("Estimativa de π após %,d amostras: %.6f%n", samples, piEstimate);
        System.out.printf("Tempo de execução: %.3f segundos%n", duration);
    }
}
