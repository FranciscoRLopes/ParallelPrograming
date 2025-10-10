package Knapsack.MasterWorker;
import Knapsack.Individual;
class KTask {
    final int lo, hi;
    final Individual[] current;
    final Individual[] next;
    final double probMutation;
    final int tournamentSize;
    final int popSize;

    KTask(int lo, int hi, Individual[] current, Individual[] next, double probMutation, int tournamentSize, int popSize) {
        this.lo = lo;
        this.hi = hi;
        this.current = current;
        this.next = next;
        this.probMutation = probMutation;
        this.tournamentSize = tournamentSize;
        this.popSize = popSize;
    }
}