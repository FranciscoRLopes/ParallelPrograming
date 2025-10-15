package Knapsack;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.*;

public class KnapsackGAExecuter {
    private static final int N_GENERATIONS = 500;
    private static final int POP_SIZE = 100_000;
    private static final double PROB_MUTATION = 0.5;
    private static final int TOURNAMENT_SIZE = 3;
    private final int nThreads;


    private Individual[] population;

    public KnapsackGAExecuter(int nThreads) {
        this.nThreads = Math.max(1, nThreads);
        this.population = new Individual[POP_SIZE];
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

    public long run() {
        long t0 = System.nanoTime();
        ExecutorService pool = Executors.newFixedThreadPool(nThreads);
        try {
            for (int gen = 0; gen < N_GENERATIONS; gen++) {
                final Individual[] current = this.population;
                final Individual[] next = new Individual[POP_SIZE];
                final int chunk = Math.max(512, POP_SIZE / (nThreads * 8));

                List<Callable<Void>> tasks = new ArrayList<>();
                for (int start = 0; start < POP_SIZE; start += chunk) {
                    final int lo = start;
                    final int hi = Math.min(POP_SIZE, start + chunk);
                    tasks.add(() -> {
                        var r = ThreadLocalRandom.current();
                        for (int i = lo; i < hi; i++) {
                            Individual p1 = tournamentSelect(current);
                            Individual p2 = tournamentSelect(current);
                            Individual child = p1.crossoverWith(p2, r);
                            if (r.nextDouble() < PROB_MUTATION) child.mutate(r);
                            child.measureFitness();
                            next[i] = child;
                        }
                        return null;
                    });
                }


                for (Future<Void> f : pool.invokeAll(tasks)) f.get();

                this.population = next;


                System.out.println("Generation " + gen + " best fitness: " + bestOfPopulation().fitness);
            }
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(ie);
        } catch (ExecutionException ee) {
            throw new RuntimeException(ee.getCause());
        } finally {
            pool.shutdown();
        }
        return System.nanoTime() - t0;
    }

    public int getBestFitness() {
        return bestOfPopulation().fitness;
    }
}
