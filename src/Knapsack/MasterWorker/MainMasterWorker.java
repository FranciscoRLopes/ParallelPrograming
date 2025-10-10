package Knapsack.MasterWorker;

public class MainMasterWorker {
    public static void main(String[] args) {
        int nWorkers = Runtime.getRuntime().availableProcessors();
        int chunkSize = 2048;
        if (args.length >= 1) try { nWorkers = Integer.parseInt(args[0]); } catch (Exception ignored) {}
        if (args.length >= 2) try { chunkSize = Integer.parseInt(args[1]); } catch (Exception ignored) {}
        KnapsackGAMasterWorker ga = new KnapsackGAMasterWorker(nWorkers, chunkSize);
        ga.run();
    }
}