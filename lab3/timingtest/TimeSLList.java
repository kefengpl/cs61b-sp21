package timingtest;
import edu.princeton.cs.algs4.Stopwatch;
import org.checkerframework.checker.units.qual.A;

/**
 * Created by hug.
 */
public class TimeSLList {
    private static void printTimingTable(AList<Integer> Ns, AList<Double> times, AList<Integer> opCounts) {
        System.out.printf("%12s %12s %12s %12s\n", "N", "time (s)", "# ops", "microsec/op");
        System.out.printf("------------------------------------------------------------\n");
        for (int i = 0; i < Ns.size(); i += 1) {
            int N = Ns.get(i);
            double time = times.get(i);
            int opCount = opCounts.get(i);
            double timePerOp = time / opCount * 1e6;
            System.out.printf("%12d %12.2f %12d %12.2f\n", N, time, opCount, timePerOp);
        }
    }

    public static void main(String[] args) {
        timeGetLast();
    }

    /**
     * 统计双向链表获取最后一个数值所需要的时间
     * getLast会运行M = 10000次
     * */
    public static void timeGetLast() {
        int baseN = 1000;
        int M = 10000;
        AList<Integer> nList = new AList<>();
        AList<Double> timeConsume = new AList<>();
        AList<Integer> opCounts = new AList<>();
        for (int i = 0; i < 8; ++i) {
            nList.addLast(baseN);
            opCounts.addLast(M);
            SLList<Integer> testList = new SLList<>();
            for (int j = 0; j < baseN; ++j) {
                testList.addLast(996);
            }
            Stopwatch sw = new Stopwatch();
            for (int k = 0; k < M; ++k) {
                testList.getLast();
            }
            double timeInSecond = sw.elapsedTime();
            timeConsume.addLast(timeInSecond);
            baseN *= 2;
        }
        printTimingTable(nList, timeConsume, opCounts);
    }

}
