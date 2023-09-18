package deque;

import org.junit.Test;

import java.util.Comparator;
import static org.junit.Assert.*;

public class MaxArrayDequeTest {
    public static class IntegerComparator implements Comparator<Integer> {
        public int compare(Integer x, Integer y) {
            // 在这里定义比较规则，返回负数表示x < y，返回0表示x == y，返回正数表示x > y
            return x.compareTo(y);
        }
    }
    public static class InvIntegerComparator implements Comparator<Integer> {
        public int compare(Integer x, Integer y) {
            // 在这里定义比较规则，返回负数表示x > y，返回0表示x == y，返回正数表示x < y
            return y.compareTo(x);
        }
    }

    @Test
    public void simpleTest() {
        Comparator<Integer> c = new IntegerComparator();
        Comparator<Integer> inv_c = new InvIntegerComparator();
        MaxArrayDeque<Integer> deque = new MaxArrayDeque<>(c);
        deque.addLast(5);
        deque.addLast(10);
        deque.addLast(8);
        deque.addLast(9);
        assertEquals(10, (long) deque.max());
        assertEquals(5, (long) deque.max(inv_c));
    }
}
