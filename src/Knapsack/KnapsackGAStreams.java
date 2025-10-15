package Knapsack;


import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;

public class KnapsackGAStreams {

    private static final int N_GENERATIONS = 500;
    private static final int POP_SIZE = 100000;
    private static final double PROB_MUTATION = 0.5;
    private static final int TOURNAMENT_SIZE = 3;

    private Individual[] population = new Individual[POP_SIZE];

    public KnapsackGAStreams() {
        this.population = new Individual[POP_SIZE];
        populateInitialPopulationRandomly();
    }

    private void populateInitialPopulationRandomly() {
        Random r = new Random();
        for (int i = 0; i < POP_SIZE; i++) {
            population[i] = Individual.createRandom(r);
            population[i].measureFitness(); // added here
        }
    }

    private Individual tournamentSelect(Individual[] src) {
        Random r = ThreadLocalRandom.current();
        Individual best = null;
        for (int i = 0; i < TOURNAMENT_SIZE; i++) {
            Individual cand = src[r.nextInt(POP_SIZE)];
            if (best == null || cand.fitness > best.fitness) {
                best = cand;
            }
        }
        return best;
    }

    private Individual bestOfPopulation() {
        Individual best = population[0];
        for (int i = 1; i < POP_SIZE; i++) {
            if (population[i].fitness > best.fitness) best = population[i];
        }
        return best;
    }

    public void run() {
        long start = System.nanoTime();

        for (int gen = 0; gen < N_GENERATIONS; gen++) {
            final Individual[] current = this.population;
            final Individual[] next = new Individual[POP_SIZE];

            IntStream.range(0, POP_SIZE)
                    .parallel()
                    .forEach(i -> {
                        Random r = ThreadLocalRandom.current();
                        Individual p1 = tournamentSelect(current);
                        Individual p2 = tournamentSelect(current);
                        Individual child = p1.crossoverWith(p2, r);
                        if (r.nextDouble() < PROB_MUTATION) {
                            child.mutate(r);
                        }
                        child.measureFitness();   // added here
                        next[i] = child;
                    });

            this.population = next;


            Individual best = bestOfPopulation();
            System.out.println("Generation " + gen + " best fitness: " + best.fitness);
        }

        long end = System.nanoTime();
        double seconds = (end - start) / 1_000_000_000.0;
        System.out.printf("Total time: %.3f seconds%n", seconds);
    }
}
