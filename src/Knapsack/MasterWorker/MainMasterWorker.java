package Knapsack.MasterWorker;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;

public class MainMasterWorker{

    private static final int DEFAULT_RUNS = 30;
    private static final int DEFAULT_CHUNK_SIZE = 2048;
    private static final int[] DEFAULT_THREADS = {1, 2, 4, 8, 16};

    public static void main(String[] args) {
        int RUNS = DEFAULT_RUNS;
        int chunkSize = DEFAULT_CHUNK_SIZE;
        int[] THREADS = Arrays.copyOf(DEFAULT_THREADS, DEFAULT_THREADS.length);


        System.out.println("=== Teste de performance (Master/Worker) ===");
        System.out.println("RUNS=" + RUNS + " | chunkSize=" + chunkSize + " | THREADS=" + Arrays.toString(THREADS));


        String csvPerRun = "mw_results.csv";
        String csvResumo = "mw_summary.csv";

        try (BufferedWriter perRun = new BufferedWriter(new FileWriter(csvPerRun));
             BufferedWriter resumo = new BufferedWriter(new FileWriter(csvResumo))) {


            perRun.write("threads,run,seconds\n");
            resumo.write("threads,avg_seconds,std_seconds,min_seconds,max_seconds\n");

            for (int numThreads : THREADS) {
                System.out.println("\n--- Executando com " + numThreads + " worker(s) ---");

                double[] times = new double[RUNS];


                for (int i = 0; i < RUNS; i++) {
                    long t0 = System.nanoTime();

                    KnapsackGAMasterWorker ga = new KnapsackGAMasterWorker(numThreads, chunkSize);
                    ga.run();

                    double elapsed = (System.nanoTime() - t0) / 1_000_000_000.0; // segundos
                    times[i] = elapsed;

                    System.out.println("Run " + (i + 1) + ": " + String.format("%.3f", elapsed) + " segundos");
                    perRun.write(numThreads + "," + (i + 1) + "," + String.format("%.6f", elapsed) + "\n");
                }

                // Estatísticas
                double sum = 0, min = Double.POSITIVE_INFINITY, max = Double.NEGATIVE_INFINITY;
                for (double t : times) {
                    sum += t;
                    min = Math.min(min, t);
                    max = Math.max(max, t);
                }
                double avg = sum / RUNS;
                double s2 = 0;
                for (double t : times) s2 += (t - avg) * (t - avg);
                double std = Math.sqrt(s2 / Math.max(1, RUNS - 1));

                String linhaResumo = String.format(
                        "%d,%.6f,%.6f,%.6f,%.6f",
                        numThreads, avg, std, min, max
                );
                resumo.write(linhaResumo + "\n");

                System.out.println(String.format(
                        "=> Threads: %d | Média: %.2f s | Desvio-padrão: %.2f | Min: %.2f | Max: %.2f",
                        numThreads, avg, std, min, max
                ));
            }

            System.out.println("\n=== Testes concluídos ===");
            System.out.println("Resultados por run gravados em: " + csvPerRun);
            System.out.println("Resumo gravado em: " + csvResumo);

        } catch (IOException e) {
            System.err.println("Falha ao escrever CSVs: " + e.getMessage());
        }
    }
}
