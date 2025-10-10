package Knapsack.MasterWorker;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadLocalRandom;
import Knapsack.Individual;

class KWorker implements Runnable {
    private final BlockingQueue<KTask> taskQueue;
    private final BlockingQueue<Integer> resultQueue;
    private final KTask poisonPill;

    KWorker(BlockingQueue<KTask> taskQueue, BlockingQueue<Integer> resultQueue, KTask poisonPill) {
        this.taskQueue = taskQueue;
        this.resultQueue = resultQueue;
        this.poisonPill = poisonPill;
    }

    private Individual tournamentSelect(Individual[] src, int popSize, int tournamentSize) {
        var r = ThreadLocalRandom.current();
        Individual best = null;
        for (int i = 0; i < tournamentSize; i++) {
            Individual cand = src[r.nextInt(popSize)];
            if (best == null || cand.fitness > best.fitness) best = cand;
        }
        return best;
    }

    @Override
    public void run() {
        try {
            while (true) {
                KTask task = taskQueue.take();
                if (task == poisonPill) {
                    break;
                }
                var r = ThreadLocalRandom.current();
                for (int i = task.lo; i < task.hi; i++) {
                    Individual p1 = tournamentSelect(task.current, task.popSize, task.tournamentSize);
                    Individual p2 = tournamentSelect(task.current, task.popSize, task.tournamentSize);
                    Individual child = p1.crossoverWith(p2, r);
                    if (r.nextDouble() < task.probMutation) child.mutate(r);
                    child.measureFitness();
                    task.next[i] = child;
                }
                // sinaliza conclusão de UMA tarefa (chunk)
                resultQueue.put(1);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}