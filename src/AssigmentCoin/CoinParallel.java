package AssigmentCoin;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;

public class CoinParallel {

    public static final int LIMIT = 999;
    private static final ForkJoinPool pool = new ForkJoinPool();

    private static int THRESHOLD = 16;

    public static int[] createRandomCoinSet(int N) {
        int[] r = new int[N];
        for (int i = 0; i < N; i++) {
            if (i % 10 == 0) {
                r[i] = 400;
            } else {
                r[i] = 4;
            }
        }
        return r;
    }

    public static void main(String[] args) {
        int nCores = Runtime.getRuntime().availableProcessors();
        int[] coins = createRandomCoinSet(30);
        int repeats = 20;

        int[] thresholds = {1, 2, 4, 8, 16, 32, 64};

        System.out.println("Cores;Threshold;Type;AvgTime(ms)");

        for (int t : thresholds) {
            THRESHOLD = t;
            long seqTotal = 0, parTotal = 0;

            for (int i = 0; i < repeats; i++) {
                // --- Sequencial ---
                long seqStart = System.nanoTime();
                int rs = seq(coins, 0, 0);
                long seqEnd = System.nanoTime() - seqStart;
                seqTotal += seqEnd;

                // --- Paralelo ---
                long parStart = System.nanoTime();
                int rp = par(coins, 0, 0);
                long parEnd = System.nanoTime() - parStart;
                parTotal += parEnd;

                if (rp != rs) {
                    System.out.println("Wrong Result!");
                    System.exit(-1);
                }
            }

            double seqAvg = seqTotal / (double) repeats / 1_000_000;
            double parAvg = parTotal / (double) repeats / 1_000_000;

            System.out.printf("%d;%d;Sequential;%.3f%n", nCores, t, seqAvg);
            System.out.printf("%d;%d;Parallel;%.3f%n", nCores, t, parAvg);
        }
    }

    // === Versão Sequencial ===
    private static int seq(int[] coins, int index, int acc) {
        if (index >= coins.length) {
            return acc < LIMIT ? acc : -1;
        }
        if (acc + coins[index] > LIMIT) return -1;

        int a = seq(coins, index + 1, acc);
        int b = seq(coins, index + 1, acc + coins[index]);
        return Math.max(a, b);
    }

    // === Versão Paralela (usa o ForkJoinPool global) ===
    private static int par(int[] coins, int index, int acc) {
        return pool.invoke(new CoinTask(coins, index, acc));
    }

    // === Classe interna ForkJoin ===
    static class CoinTask extends RecursiveTask<Integer> {
        private final int[] coins;
        private final int index;
        private final int acc;

        CoinTask(int[] coins, int index, int acc) {
            this.coins = coins;
            this.index = index;
            this.acc = acc;
        }

        @Override
        protected Integer compute() {
            if (index >= coins.length) return acc < LIMIT ? acc : -1;
            if (acc + coins[index] > LIMIT) return -1;

            // threshold dinâmico
            if (coins.length - index <= THRESHOLD) {
                return seq(coins, index, acc);
            }

            CoinTask left = new CoinTask(coins, index + 1, acc);
            CoinTask right = new CoinTask(coins, index + 1, acc + coins[index]);

            left.fork();
            int rightResult = right.compute();
            int leftResult = left.join();

            return Math.max(leftResult, rightResult);
        }
    }
}
