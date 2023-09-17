package deque;

import org.junit.Test;
import static org.junit.Assert.*;

public class ArrayDequeTest {
    @Test
    /* 测试“环”状移动功能 */
    public void cycleMoveTest() {
        ArrayDeque<Integer> deque = new ArrayDeque<>();
        assertEquals(7, (long)deque.getMoveCycle(ArrayDeque.Direction.LEFT, ArrayDeque.DequeLocation.FRONT));
        assertEquals(1,(long)deque.getMoveCycle(ArrayDeque.Direction.RIGHT, ArrayDeque.DequeLocation.TAIL));
    }

    @Test
    /* 随机测试实现的不同操作 */
    public void randomMethodTest() {
        LinkedListDeque<Integer> lld1 = new LinkedListDeque<Integer>();
        assertNull(lld1.get(0));
        assertNull(lld1.get(4000));
        assertNull(lld1.removeLast());
        lld1.addFirst(5);
        lld1.addLast(10);
        lld1.addFirst(15);
        assertEquals((long) lld1.removeFirst(),15);
        //lld1.printDeque();
        assertEquals((long) lld1.removeLast(),10);
        assertEquals((long) lld1.removeLast(),5);
        assertNull(lld1.removeLast());
        assertNull(lld1.removeFirst());
        lld1.addFirst(11555000);
        lld1.addLast(259);
        LinkedListDeque<Integer> lld2 = new LinkedListDeque<Integer>();
        lld2.addFirst(11555000);
        lld2.addLast(259);
        for (Integer elem : lld1) {
            System.out.print(elem + " ");
        }
        System.out.println();
        assertTrue(lld1.equals(lld2));
    }
}
