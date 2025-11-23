package Assigment3_Java;

import java.util.Random;

public class KnapsackGAChannels {

    // Mesmos parâmetros do sequencial
    private static final int N_GENERATIONS = 500;
    private static final int POP_SIZE = 100000;
    private static final double PROB_MUTATION = 0.5;
    private static final int TOURNAMENT_SIZE = 3;

    // ---------------- MENSAGENS ----------------

    interface Message { }

    static class GenerationMsg implements Message {
        final Individual[] population;
        final int generation;

        GenerationMsg(Individual[] population, int generation) {
            this.population = population;
            this.generation = generation;
        }
    }

    static class EvaluatedGenerationMsg implements Message {
        final Individual[] population;
        final int generation;

        EvaluatedGenerationMsg(Individual[] population, int generation) {
            this.population = population;
            this.generation = generation;
        }
    }

    static class StopMsg implements Message { }

    // ---------------- WORKERS ----------------

    /**
     * Worker que recebe uma GenerationMsg e calcula measureFitness()
     * para todos os indivíduos.
     */
    static class FitnessWorker implements Runnable {

        private final Channel<Message> in;
        private final Channel<Message> out;

        FitnessWorker(Channel<Message> in, Channel<Message> out) {
            this.in = in;
            this.out = out;
        }

        @Override
        public void run() {
            while (true) {
                Message msg = in.receive();
                if (msg == null) continue;

                if (msg instanceof StopMsg) {
                    // Propaga o Stop e termina
                    out.send(msg);
                    break;
                }

                if (msg instanceof GenerationMsg gen) {
                    for (Individual ind : gen.population) {
                        ind.measureFitness();
                    }
                    out.send(new EvaluatedGenerationMsg(gen.population, gen.generation));
                }
            }
        }
    }

    /**
     * Worker que recebe populações já avaliadas, imprime / segue
     * e decide quando parámos (última geração).
     */
    static class LoggerWorker implements Runnable {

        private final Channel<Message> in;
        private final Channel<Message> out;
        private final int maxGenerations;

        LoggerWorker(Channel<Message> in, Channel<Message> out, int maxGenerations) {
            this.in = in;
            this.out = out;
            this.maxGenerations = maxGenerations;
        }

        @Override
        public void run() {
            Individual bestOverall = null;

            while (true) {
                Message msg = in.receive();
                if (msg == null) continue;

                if (msg instanceof StopMsg) {
                    // Se algum dia recebêssemos Stop “de trás”
                    break;
                }

                if (msg instanceof EvaluatedGenerationMsg eval) {
                    Individual best = bestOfPopulation(eval.population);
                    if (bestOverall == null || best.fitness > bestOverall.fitness) {
                        bestOverall = best;
                    }

                    System.out.println("Generation " + eval.generation +
                            " best fitness = " + best.fitness);

                    // Se chegámos à última geração, pára o pipeline
                    if (eval.generation >= maxGenerations - 1) {
                        System.out.println("Finished after " + (eval.generation + 1)
                                + " generations. Best overall fitness = "
                                + bestOverall.fitness);
                        out.send(new StopMsg());
                        break;
                    } else {
                        // Continua o pipeline: manda a população avaliada
                        out.send(eval);
                    }
                }
            }
        }

        private static Individual bestOfPopulation(Individual[] population) {
            Individual best = population[0];
            for (Individual other : population) {
                if (other.fitness > best.fitness) {
                    best = other;
                }
            }
            return best;
        }
    }

    /**
     * Worker que recebe populações avaliadas e gera a próxima geração
     * via torneio + crossover + mutação, enviando uma GenerationMsg.
     */
    static class ReproductionWorker implements Runnable {

        private final Channel<Message> in;
        private final Channel<Message> out;
        private final Random r;

        ReproductionWorker(Channel<Message> in, Channel<Message> out) {
            this.in = in;
            this.out = out;
            this.r = new Random();
        }

        @Override
        public void run() {
            while (true) {
                Message msg = in.receive();
                if (msg == null) continue;

                if (msg instanceof StopMsg) {
                    // Propaga e termina
                    out.send(msg);
                    break;
                }

                if (msg instanceof EvaluatedGenerationMsg eval) {
                    Individual[] oldPop = eval.population;
                    Individual[] nextPop = new Individual[POP_SIZE];

                    for (int i = 0; i < POP_SIZE; i++) {
                        Individual dad = tournament(oldPop, r);
                        Individual mom = tournament(oldPop, r);

                        Individual child = dad.crossoverWith(mom, r);

                        if (r.nextDouble() < PROB_MUTATION) {
                            child.mutate(r);
                        }

                        nextPop[i] = child;
                    }

                    out.send(new GenerationMsg(nextPop, eval.generation + 1));
                }
            }
        }

        private static Individual tournament(Individual[] population, Random r) {
            Individual best = population[r.nextInt(population.length)];
            for (int i = 0; i < TOURNAMENT_SIZE; i++) {
                Individual other = population[r.nextInt(population.length)];
                if (other.fitness > best.fitness) {
                    best = other;
                }
            }
            return best;
        }
    }

    // ---------------- EXECUÇÃO (API PÚBLICA) ----------------

    public void run() {
        // Canais do pipeline
        Channel<Message> chFitnessIn = new Channel<>();
        Channel<Message> chFitnessToLogger = new Channel<>();
        Channel<Message> chLoggerToRepro = new Channel<>();

        // Fitness -> Logger -> Repro -> Fitness (ciclo)
        Channel<Message> chReproToFitness = chFitnessIn;

        Thread fitnessThread = new Thread(
                new FitnessWorker(chFitnessIn, chFitnessToLogger),
                "fitness-worker"
        );
        Thread loggerThread = new Thread(
                new LoggerWorker(chFitnessToLogger, chLoggerToRepro, N_GENERATIONS),
                "logger-worker"
        );
        Thread reproThread = new Thread(
                new ReproductionWorker(chLoggerToRepro, chReproToFitness),
                "reproduction-worker"
        );

        fitnessThread.start();
        loggerThread.start();
        reproThread.start();

        // População inicial
        Random r = new Random();
        Individual[] initialPopulation = new Individual[POP_SIZE];
        for (int i = 0; i < POP_SIZE; i++) {
            initialPopulation[i] = Individual.createRandom(r);
        }

        long start = System.currentTimeMillis();

        // Entra no pipeline
        chFitnessIn.send(new GenerationMsg(initialPopulation, 0));

        // Espera que o Stop atravesse o pipeline todo e que as threads terminem
        try {
            fitnessThread.join();
            loggerThread.join();
            reproThread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        long end = System.currentTimeMillis();
        System.out.println("Total time (channels version): " + (end - start) + " ms");
    }
}
