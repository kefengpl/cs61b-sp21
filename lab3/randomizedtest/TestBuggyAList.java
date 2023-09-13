package randomizedtest;

import edu.princeton.cs.algs4.StdRandom;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Created by hug.
 */
public class TestBuggyAList {
    /**
     * 测试函数：将两个AList进行对比，先添加三个相同的元素
     * 再移除三个相同的元素，最后考察结果是否相等
     * */
    @Test
    public void testThreeAddThreeRemove() {
        AListNoResizing<Integer> normalAList = new AListNoResizing<>();
        BuggyAList<Integer> bugAlist = new BuggyAList<>();
        for (int i = 0; i < 3; ++i) {
            normalAList.addLast(i + 4);
            bugAlist.addLast(i + 4);
        }
        for (int i = 0; i < 3; ++i) {
            assertEquals(normalAList.removeLast(), bugAlist.removeLast());
        }
    }

    @Test
    public void randomizedTest() {
        AListNoResizing<Integer> L = new AListNoResizing<>();
        BuggyAList<Integer> bugL = new BuggyAList<>();
        int N = 5000;
        for (int i = 0; i < N; i += 1) {
            // uniform是左开右闭区间
            int operationNumber = StdRandom.uniform(0, 4);
            if (operationNumber == 0) {
                // addLast
                int randVal = StdRandom.uniform(0, 100);
                L.addLast(randVal);
                bugL.addLast(randVal);
            } else if (operationNumber == 1) {
                // size
                int size = L.size();
                int bugLSize = bugL.size();
                assertEquals(size, bugLSize);
            } else if (operationNumber == 2) {
                assertEquals(L.size() > 0 ? L.getLast() : null,
                             bugL.size() > 0 ? bugL.getLast() : null);
            } else if (operationNumber == 3) {
                assertEquals(L.size() > 0 ? L.removeLast() : null,
                        bugL.size() > 0 ? bugL.removeLast() : null);
            }
        }
    }
}
