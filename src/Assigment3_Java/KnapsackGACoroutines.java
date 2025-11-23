package Assigment3_Java;


import java.util.Random;
import java.util.concurrent.CountDownLatch;

public class KnapsackGACoroutines {

    private static final int N_GENERATIONS = 500;
    private static final int POP_SIZE = 100000;
    private static final double PROB_MUTATION = 0.5;
    private static final int TOURNAMENT_SIZE = 3;

    private final Random r = new Random();
    private Individual[] population = new Individual[POP_SIZE];

    public KnapsackGACoroutines() {
        populateInitialPopulationRandomly();
    }

    private void populateInitialPopulationRandomly() {
        for (int i = 0; i < POP_SIZE; i++) {
            population[i] = Individual.createRandom(r);
        }
    }

    public void run() {
        long start = System.currentTimeMillis();

        for (int generation = 0; generation < N_GENERATIONS; generation++) {

            // --------- FITNESS EM CO-ROTINAS (VIRTUAL THREADS) ---------
            CountDownLatch latch = new CountDownLatch(POP_SIZE);

            for (int i = 0; i < POP_SIZE; i++) {
                final Individual ind = population[i];
                Thread.ofVirtual().start(() -> {
                    ind.measureFitness();
                    latch.countDown();
                });
            }

            try {
                latch.await(); // Espera todas as co-rotinas desta geração
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // --------- MELHOR INDIVÍDUO / LOG ---------
            Individual best = bestOfPopulation();
            System.out.println("Generation " + generation +
                    " best fitness = " + best.fitness);

            // --------- REPRODUÇÃO (SELEÇÃO + CROSSOVER + MUTAÇÃO) ---------
            if (generation < N_GENERATIONS - 1) {
                reproduce();
            }
        }

        long end = System.currentTimeMillis();
        System.out.println("Total time (coroutines version): " + (end - start) + " ms");
    }

    private void reproduce() {
        Individual[] newPopulation = new Individual[POP_SIZE];

        for (int i = 0; i < POP_SIZE; i++) {
            Individual dad = tournament(TOURNAMENT_SIZE, r);
            Individual mom = tournament(TOURNAMENT_SIZE, r);

            Individual child = dad.crossoverWith(mom, r);

            if (r.nextDouble() < PROB_MUTATION) {
                child.mutate(r);
            }

            newPopulation[i] = child;
        }

        this.population = newPopulation;
    }

    private Individual tournament(int tournamentSize, Random r) {
        // Mesmo comportamento do teu KnapsackGA sequencial
        Individual best = population[r.nextInt(POP_SIZE)];
        for (int i = 0; i < tournamentSize; i++) {
            Individual other = population[r.nextInt(POP_SIZE)];
            if (other.fitness > best.fitness) {
                best = other;
            }
        }
        return best;
    }

    private Individual bestOfPopulation() {
        Individual best = population[0];
        for (Individual other : population) {
            if (other.fitness > best.fitness) {
                best = other;
            }
        }
        return best;
    }
}
