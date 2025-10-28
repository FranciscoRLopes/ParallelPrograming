package AssigmentCoin;

import java.util.concurrent.RecursiveTask;
import java.util.concurrent.ForkJoinPool;

public class CoinParallel {

    public static final int LIMIT = 999;

    public static int[] createRandomCoinSet(int N) {
        int[] r = new int[N];
        for (int i = 0; i < N ; i++) {
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

        int repeats = 40;
        for (int i = 0; i < repeats; i++) {
            long seqInitialTime = System.nanoTime();
            int rs = seq(coins, 0, 0);
            long seqEndTime = System.nanoTime() - seqInitialTime;
            System.out.println(nCores + ";Sequential;" + seqEndTime / 1_000_000);

            long parInitialTime = System.nanoTime();
            int rp = par(coins, 0, 0);
            long parEndTime = System.nanoTime() - parInitialTime;
            System.out.println(nCores + ";Parallel;" + parEndTime / 1_000_000);

            if (rp != rs) {
                System.out.println("Wrong Result!");
                System.exit(-1);
            }
        }
    }


    private static int seq(int[] coins, int index, int accumulator) {
        if (index >= coins.length) {
            if (accumulator < LIMIT) {
                return accumulator;
            }
            return -1;
        }
        if (accumulator + coins[index] > LIMIT) {
            return -1;
        }
        int a = seq(coins, index + 1, accumulator);
        int b = seq(coins, index + 1, accumulator + coins[index]);
        return Math.max(a, b);
    }


    private static int par(int[] coins, int index, int accumulator) {
        ForkJoinPool pool = new ForkJoinPool(); // Usa número de cores do sistema
        return pool.invoke(new CoinTask(coins, index, accumulator));
    }


    static class CoinTask extends RecursiveTask<Integer> {
        private final int[] coins;
        private final int index;
        private final int accumulator;
        private static final int THRESHOLD = 3;

        CoinTask(int[] coins, int index, int accumulator) {
            this.coins = coins;
            this.index = index;
            this.accumulator = accumulator;
        }

        @Override
        protected Integer compute() {
            if (index >= coins.length) {
                if (accumulator < LIMIT) return accumulator;
                return -1;
            }

            if (accumulator + coins[index] > LIMIT) {
                return -1;
            }

            // --- Caso base: se o problema for pequeno, faz sequencial ---
            //if (coins.length - index <= THRESHOLD) {
            //    return seq(coins, index, accumulator);
            //}

            // --- Divide o trabalho ---
            CoinTask left = new CoinTask(coins, index + 1, accumulator);
            CoinTask right = new CoinTask(coins, index + 1, accumulator + coins[index]);

            // --- Executa em paralelo ---
            left.fork();
            int rightResult = right.compute(); // calcula diretamente o "right"
            int leftResult = left.join();      // espera o resultado do "left"

            return Math.max(leftResult, rightResult);
        }
    }
}
