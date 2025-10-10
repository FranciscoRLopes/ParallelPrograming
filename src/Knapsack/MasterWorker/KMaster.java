package Knapsack.MasterWorker;

import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

class KMaster {
    private final BlockingQueue<KTask> taskQueue;
    private final BlockingQueue<Integer> resultQueue;
    private final Thread[] workers;
    private final KTask poisonPill;

    KMaster(int numWorkers) {
        this.taskQueue = new ArrayBlockingQueue<>(Math.max(32, numWorkers * 4));
        this.resultQueue = new ArrayBlockingQueue<>(Math.max(32, numWorkers * 4));
        this.workers = new Thread[numWorkers];
        this.poisonPill = new KTask(-1, -1, null, null, 0.0, 0, 0);

        for (int i = 0; i < numWorkers; i++) {
            workers[i] = new Thread(new KWorker(taskQueue, resultQueue, poisonPill), "KWorker-" + i);
            workers[i].start();
        }
    }

    void submitTask(KTask t) throws InterruptedException {
        taskQueue.put(t);
    }

    void awaitTasks(int nTasks) throws InterruptedException {
        int done = 0;
        while (done < nTasks) {
            Integer one = resultQueue.take();
            if (one != null) done += one;
        }
    }

    void shutdown() throws InterruptedException {
        for (int i = 0; i < workers.length; i++) taskQueue.put(poisonPill);
        for (Thread w : workers) w.join();
    }
}