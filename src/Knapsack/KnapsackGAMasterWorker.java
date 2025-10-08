package Knapsack;

import java.util.Random;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class KnapsackGAMasterWorker {
    private static final int N_GENERATIONS = 500;
    private static final int POP_SIZE = 100_000;
    private static final double PROB_MUTATION = 0.5;
    private static final int TOURNAMENT_SIZE = 3;

    private final int nWorkers = 10;
    private final int chunkSize = 1024; // p.ex. 1024–8192
    private final BlockingQueue<int[]> queue; // tarefas: [lo, hi)
    private final Worker[] workers;

    private volatile Individual[] current; // lido pelos workers
    private volatile Individual[] next;    // escrito pelos workers (índices disjuntos)
    private final AtomicBoolean running = new AtomicBoolean(true);

    private Individual[] population;



    public KnapsackGAMasterWorker() {
        this.queue = new ArrayBlockingQueue<>(Math.max(32, nWorkers * 4));
        this.population = new Individual[POP_SIZE];
        populateInitialPopulationRandomly();

        // Cria e arranca os workers (threads fixas reutilizadas em todas as gerações)
        this.workers = new Worker[nWorkers];
        for (int w = 0; w < nWorkers; w++) {
            workers[w] = new Worker();
            workers[w].start();
        }
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

    public void run() {
        long t0 = System.nanoTime();

        for (int gen = 0; gen < N_GENERATIONS; gen++) {
            this.current = this.population;
            this.next = new Individual[POP_SIZE];

            // Prepara latch = nº de tarefas que vamos enfileirar
            int nTasks = (POP_SIZE + chunkSize - 1) / chunkSize;
            CountDownLatch latch = new CountDownLatch(nTasks);

            // Enfileira tarefas (intervalos [lo, hi))
            for (int start = 0; start < POP_SIZE; start += chunkSize) {
                int lo = start;
                int hi = Math.min(POP_SIZE, start + chunkSize);
                try {
                    queue.put(new int[]{lo, hi});
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(e);
                }
            }

            // Dá o latch aos workers para esta geração
            for (Worker w : workers) w.setLatch(latch);

            // Espera terminar a geração
            try { latch.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }

            // Swap e print
            this.population = this.next;
            Individual best = bestOfPopulation();
            System.out.println("Generation " + gen + " best fitness: " + best.fitness);
        }

        // Parar workers
        running.set(false);
        // mandar “poison” suficiente para desbloquear os takes
        for (int i = 0; i < nWorkers; i++) {
            try { queue.put(new int[]{-1, -1}); } catch (InterruptedException ignored) {}
        }
        for (Worker w : workers) {
            try { w.join(); } catch (InterruptedException ignored) {}
        }

        long t1 = System.nanoTime();
        System.out.printf("Total time: %.3f s%n", (t1 - t0)/1e9);
    }

    private class Worker extends Thread {
        private volatile CountDownLatch latch;

        void setLatch(CountDownLatch latch) { this.latch = latch; }

        @Override public void run() {
            while (running.get()) {
                int[] task;
                try {
                    task = queue.take();
                } catch (InterruptedException e) {
                    if (!running.get()) break;
                    continue;
                }
                int lo = task[0], hi = task[1];
                if (lo < 0) break; // poison

                var r = ThreadLocalRandom.current();
                for (int i = lo; i < hi; i++) {
                    Individual p1 = tournamentSelect(current);
                    Individual p2 = tournamentSelect(current);
                    Individual child = p1.crossoverWith(p2, r);
                    if (r.nextDouble() < PROB_MUTATION) child.mutate(r);
                    child.measureFitness();
                    next[i] = child;
                }
                // sinaliza conclusão da tarefa
                latch.countDown();
            }
        }
    }
}
