package Knapsack;


public class Main {
	public static void main(String[] args) {
        int RUNS = 10;
        long[] times = new long[RUNS];

        for (int i = 0; i < RUNS; i++) {
            long t0 = System.nanoTime();
            KnapsackGA ga = new KnapsackGA();
            //KnapsackGAStreams ga = new KnapsackGAStreams();
            //KnapsackGAManual ga = new KnapsackGAManual();
            ga.run();
            times[i] = (System.nanoTime() - t0) / 1_000_000_000; // ms
            System.out.println("Run " + (i+1) + ": " + times[i] + "segundos");
        }

        //Calculos com todas as runs
        long sum = 0, min = Long.MAX_VALUE, max = Long.MIN_VALUE;
        for (long t : times) { sum += t; min = Math.min(min, t); max = Math.max(max, t); }
        double avg = sum / (double) RUNS;

        // desvio padrão
        double s2 = 0;
        for (long t : times) s2 += (t - avg) * (t - avg);
        double std = Math.sqrt(s2 / (RUNS - 1));

        System.out.printf("Média: %.2f s | Desvio-padrão: %.2f | Min: %d | Max: %d%n", avg, std, min, max);
	}
}
