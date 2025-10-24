public class Coin {

	public static final int LIMIT = 999;
	
	public static int[] createRandomCoinSet(int N) {
		int[] r = new int[N];
		for (int i = 0; i < N ; i++) {
			if (i % 10 == 0) {
				r[i] = 400;
			} else {
				r[i] = 4;
			}
		}
		return r;
	}

	public static void main(String[] args) {
		int nCores = Runtime.getRuntime().availableProcessors();

		int[] coins = createRandomCoinSet(30);

		int repeats = 40;
		for (int i=0; i<repeats; i++) {
			long seqInitialTime = System.nanoTime();
			int rs = seq(coins, 0, 0);
			long seqEndTime = System.nanoTime() - seqInitialTime;
			System.out.println(nCores + ";Sequential;" + seqEndTime);
			
			long parInitialTime = System.nanoTime();
			int rp = seq(coins, 0, 0);
			long parEndTime = System.nanoTime() - parInitialTime;
			System.out.println(nCores + ";Parallel;" + parEndTime);
			
			if (rp != rs) {
				System.out.println("Wrong Result!");
				System.exit(-1);
			}
		}

	}

	private static int seq(int[] coins, int index, int accumulator) {
		
		if (index >= coins.length) {
			if (accumulator < LIMIT) {
				return accumulator;
			}
			return -1;
		}
		if (accumulator + coins[index] > LIMIT) {
			return -1;
		}
		int a = seq(coins, index+1, accumulator);
		int b = seq(coins, index+1, accumulator + coins[index]);
		return Math.max(a,  b);
	}
	
	private static int par(int[] coins, int index, int accumulator) {
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return -1;
	}
}
