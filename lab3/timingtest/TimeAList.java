package timingtest;
import edu.princeton.cs.algs4.Stopwatch;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by hug.
 */
public class TimeAList {
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
        timeAListConstruction();
    }

    /**
     * 用于统计AList函数运行不同N的addLast时间的消耗
     * */
    public static void timeAListConstruction() {
        int[] testList = new int[]{1000, 2000, 4000, 8000,
                                   16000, 32000, 64000, 128000};
        AList<Integer> Ns = new AList<>();
        AList<Double> timeConsume = new AList<>();
        for (int i = 0; i < testList.length; ++i) {
            Ns.addLast(testList[i]);
            AList<Integer> testObj = new AList<>();
            // 使用stopwatch记录消逝的时间
            Stopwatch sw = new Stopwatch();
            for (int j = 0; j < testList[i]; ++j) {
                testObj.addLast(996);
            }
            double timeInSecond = sw.elapsedTime();
            timeConsume.addLast(timeInSecond);
        }
        printTimingTable(Ns, timeConsume, Ns);
    }
}
