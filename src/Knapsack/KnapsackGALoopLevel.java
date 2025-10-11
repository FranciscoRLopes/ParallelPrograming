package Knapsack;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class KnapsackGALoopLevel {
    private static final int N_GENERATIONS = 500;
    private static final int POP_SIZE = 100_000;
    private static final double PROB_MUTATION = 0.5;
    private static final int TOURNAMENT_SIZE = 3;

    private int nThreads = 8;
    private Individual[] population;

    public KnapsackGALoopLevel() {
        this.nThreads = Math.max(1, nThreads);
        this.population = new Individual[POP_SIZE];
        populateInitialPopulationRandomly();
    }

    private void populateInitialPopulationRandomly() {
        Random r = new Random();
        for (int i = 0; i < POP_SIZE; i++) {
            population[i] = Individual.createRandom(r);
            population[i].measureFitness(); // importante
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

    public void run() {
        long t0 = System.nanoTime();

        for (int gen = 0; gen < N_GENERATIONS; gen++) {
            final Individual[] current = this.population;
            final Individual[] next = new Individual[POP_SIZE];

            Thread[] threads = new Thread[nThreads];
            int chunk = (POP_SIZE + nThreads - 1) / nThreads;

            for (int tid = 0; tid < nThreads; tid++) {
                final int lo = tid * chunk;
                final int hi = Math.min(POP_SIZE, lo + chunk);

                threads[tid] = new Thread(() -> {
                    var r = ThreadLocalRandom.current();
                    for (int i = lo; i < hi; i++) {
                        Individual p1 = tournamentSelect(current);
                        Individual p2 = tournamentSelect(current);
                        Individual child = p1.crossoverWith(p2, r);
                        if (r.nextDouble() < PROB_MUTATION) child.mutate(r);
                        child.measureFitness();       // importante
                        next[i] = child;              // importante
                    }
                });
                threads[tid].start();
            }


            for (Thread th : threads) {
                try { th.join(); } catch (InterruptedException e) { throw new RuntimeException(e); }
            }

            this.population = next;


            Individual best = bestOfPopulation();
            System.out.println("Generation " + gen + " best fitness: " + best.fitness);
        }

        long t1 = System.nanoTime();
        System.out.printf("Total time: %.3f s%n", (t1 - t0)/1e9);
    }
}
