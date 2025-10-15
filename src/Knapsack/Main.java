package Knapsack;

import java.util.ArrayList;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        int RUNS = 30;
        int[] THREADS = {2, 4, 8, 16};

        System.out.println("=== Teste de performance ===");

        List<String> resultados = new ArrayList<>();

        for (int numThreads : THREADS) {
            System.out.println("\n--- Executando com " + numThreads + " thread(s) ---");

            long[] times = new long[RUNS];

            for (int i = 0; i < RUNS; i++) {
                long t0 = System.nanoTime();
                //KnapsackGA ga = new KnapsackGA();
                //KnapsackGAForkJoin ga = new KnapsackGAForkJoin();
                //KnapsackGALoopLevel ga = new KnapsackGALoopLevel();
                //KnapsackGAStreams ga = new KnapsackGAStreams();
                KnapsackGAExecuter ga = new KnapsackGAExecuter(numThreads);
                ga.run();

                long elapsed = (System.nanoTime() - t0) / 1_000_000_000; // segundos
                times[i] = elapsed;

                System.out.println("Run " + (i + 1) + ": " + elapsed + " segundos");
            }

            // Cálculos estatísticos
            long sum = 0, min = Long.MAX_VALUE, max = Long.MIN_VALUE;
            for (long t : times) {
                sum += t;
                min = Math.min(min, t);
                max = Math.max(max, t);
            }

            double avg = sum / (double) RUNS;

            double s2 = 0;
            for (long t : times) s2 += (t - avg) * (t - avg);
            double std = Math.sqrt(s2 / (RUNS - 1));


            String linha = String.format(
                    "=> Threads: %d | Média: %.2f s | Desvio-padrão: %.2f | Min: %d | Max: %d",
                    numThreads, avg, std, min, max
            );
            resultados.add(linha);
        }


        System.out.println("\n=== Testes concluídos ===\n");
        for (String linha : resultados) {
            System.out.println(linha);
        }
    }
}

