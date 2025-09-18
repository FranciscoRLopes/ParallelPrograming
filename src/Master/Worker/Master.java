package Master.Worker;
import java.util.concurrent.*;

class Master {
    private final BlockingQueue<Task> taskQueue;
    private final BlockingQueue<Integer> resultQueue;
    private final Thread[] workers;
    private final Task poisonPill;

    Master(int numWorkers) {
        this.taskQueue = new ArrayBlockingQueue<>(numWorkers);
        this.resultQueue = new ArrayBlockingQueue<>(numWorkers);
        this.workers = new Thread[numWorkers];
        this.poisonPill = new Task(-1); // objeto único representando o "fim"

        for (int i = 0; i < numWorkers; i++) {
            workers[i] = new Thread(new Worker(taskQueue, resultQueue, poisonPill));
            workers[i].start();
        }
    }

    void submitTask(Task task) throws InterruptedException {
        taskQueue.put(task);
    }

    int getResult() throws InterruptedException {
        return resultQueue.take();
    }

    void shutdown() throws InterruptedException {
        // envia a poison pill para cada worker
        for (int i = 0; i < workers.length; i++) {
            taskQueue.put(poisonPill);
        }
        // espera os workers terminarem
        for (Thread w : workers) {
            w.join();
        }
    }
}

