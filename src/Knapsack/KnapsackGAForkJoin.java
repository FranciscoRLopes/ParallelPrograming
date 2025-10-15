
package Knapsack;

import java.util.Random;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Fork/Join implementation for the Knapsack GA.
 * Signature compatible with ExperimentRunner:
 *   KnapsackGAForkJoin(int threads, long seed, int gens, int pop, double pmut, int k)
 * Methods:
 *   long run(); int getBestFitness();
 *
 * Design:
 *  - Double buffer per generation: current (read-only) -> next (write-only by index)
 *  - RecursiveAction splits ranges [lo, hi) until below threshold, then processes sequentially
 *  - RNG: ThreadLocalRandom in worker threads
 *  - One println per generation (after join)
 */
public class KnapsackGAForkJoin {

    private static final int N_GENERATIONS = 500;
    private static final int POP_SIZE = 100_000;
    private static final double PROB_MUTATION = 0.5;
    private static final int TOURNAMENT_SIZE = 3;
    private int nThreads = 8;

    private Individual[] population;
    private ForkJoinPool pool;

    public KnapsackGAForkJoin() {
        this.nThreads = Math.max(1, nThreads);
        this.population = new Individual[POP_SIZE];
        this.pool = new ForkJoinPool(this.nThreads);
        populateInitialPopulationRandomly();
    }

    private void populateInitialPopulationRandomly() {
        Random r = new Random();
        for (int i = 0; i < POP_SIZE; i++) {
            population[i] = Individual.createRandom(r);
            population[i].measureFitness();
        }
    }

    private Individual tournamentSelect(Individual[] src) {
        var r = ThreadLocalRandom.current();
        Individual best = null;
        for (int i = 0; i < TOURNAMENT_SIZE; i++) {
            Individual cand = src[r.nextInt(POP_SIZE)];
            if (best == null || cand.fitness > best.fitness) best = cand;
        }
        return best;
    }

    private Individual bestOfPopulation() {
        Individual best = population[0];
        for (int i = 1; i < POP_SIZE; i++) if (population[i].fitness > best.fitness) best = population[i];
        return best;
    }

    private class GenTask extends RecursiveAction {
        private static final int SEQ_THRESHOLD = 4096; // Fazer vários testes com números diferentes
        final Individual[] current;
        final Individual[] next;
        final int lo, hi;

        GenTask(Individual[] current, Individual[] next, int lo, int hi) {
            this.current = current; this.next = next; this.lo = lo; this.hi = hi;
        }

        @Override
        protected void compute() {
            int len = hi - lo;
            if (len <= SEQ_THRESHOLD) {
                var r = ThreadLocalRandom.current();
                for (int i = lo; i < hi; i++) {
                    Individual p1 = tournamentSelect(current);
                    Individual p2 = tournamentSelect(current);
                    Individual child = p1.crossoverWith(p2, r);
                    if (r.nextDouble() < PROB_MUTATION) child.mutate(r);
                    child.measureFitness();
                    next[i] = child;
                }
            } else {
                int mid = lo + (len >> 1);
                GenTask left = new GenTask(current, next, lo, mid);
                GenTask right = new GenTask(current, next, mid, hi);
                right.fork();
                left.compute();
                right.join();
            }
        }
    }

    public long run() {
        long t0 = System.nanoTime();
        for (int gen = 0; gen < N_GENERATIONS; gen++) {
            final Individual[] current = this.population;
            final Individual[] next = new Individual[POP_SIZE];

            pool.invoke(new GenTask(current, next, 0, POP_SIZE));

            this.population = next;

            Individual best = bestOfPopulation();
            System.out.println("Generation " + gen + " best fitness: " + best.fitness);
        }
        long t1 = System.nanoTime();
        return (t1 - t0);
    }

    public int getBestFitness() {
        return bestOfPopulation().fitness;
    }
}
