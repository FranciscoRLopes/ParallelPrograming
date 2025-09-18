package Master.Worker;

import java.util.concurrent.*;

class Worker implements Runnable {
    private final BlockingQueue<Task> taskQueue;
    private final BlockingQueue<Integer> resultQueue;
    private final Task poisonPill;

    Worker(BlockingQueue<Task> taskQueue, BlockingQueue<Integer> resultQueue, Task poisonPill) {
        this.taskQueue = taskQueue;
        this.resultQueue = resultQueue;
        this.poisonPill = poisonPill;
    }

    @Override
    public void run() {
        try {
            while (true) {
                Task task = taskQueue.take();
                if (task == poisonPill) { // se for poison pill → encerra
                    break;
                }
                int inside = Main.estimatePi(task.numSamples);
                resultQueue.put(inside);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}