package Knapsack.MasterWorker;
import Knapsack.Individual;
import java.util.Random;

public class KnapsackGAMasterWorker {
    private static final int N_GENERATIONS = 500;
    private static final int POP_SIZE = 100_000;
    private static final double PROB_MUTATION = 0.5;
    private static final int TOURNAMENT_SIZE = 3;

    private final int nWorkers;
    private final int chunkSize;
    private Individual[] population;
    private final KMaster master;

    public KnapsackGAMasterWorker(int nWorkers, int chunkSize) {
        this.nWorkers = Math.max(1, nWorkers);
        this.chunkSize = Math.max(64, chunkSize);
        this.population = new Individual[POP_SIZE];
        populateInitialPopulationRandomly();
        this.master = new KMaster(this.nWorkers);
    }

    private void populateInitialPopulationRandomly() {
        Random r = new Random();
        for (int i = 0; i < POP_SIZE; i++) {
            population[i] = Individual.createRandom(r);
            population[i].measureFitness();
        }
    }

    private Individual bestOfPopulation() {
        Individual best = population[0];
        for (int i = 1; i < POP_SIZE; i++) if (population[i].fitness > best.fitness) best = population[i];
        return best;
    }

    public void run() {
        long t0 = System.nanoTime();
        try {
            for (int gen = 0; gen < N_GENERATIONS; gen++) {
                final Individual[] current = this.population;
                final Individual[] next = new Individual[POP_SIZE];

                int nTasks = (POP_SIZE + chunkSize - 1) / chunkSize;
                for (int start = 0; start < POP_SIZE; start += chunkSize) {
                    int lo = start;
                    int hi = Math.min(POP_SIZE, start + chunkSize);
                    master.submitTask(new KTask(lo, hi, current, next, PROB_MUTATION, TOURNAMENT_SIZE, POP_SIZE));
                }

                master.awaitTasks(nTasks);

                this.population = next;
                Individual best = bestOfPopulation();
                System.out.println("Generation " + gen + " best fitness: " + best.fitness);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        } finally {
            try { master.shutdown(); } catch (InterruptedException ignored) {}
        }
        long t1 = System.nanoTime();
        System.out.printf("Total time: %.3f s%n", (t1 - t0)/1e9);
    }
}