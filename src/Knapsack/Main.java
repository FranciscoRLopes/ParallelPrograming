package Knapsack;


public class Main {
	public static void main(String[] args) {
		//KnapsackGA ga = new KnapsackGA();
        //KnapsackGAStreams ga = new KnapsackGAStreams();
        //KnapsackGAManual ga = new KnapsackGAManual();
        KnapsackGAMasterWorker ga = new KnapsackGAMasterWorker();
		ga.run();
	}
}
