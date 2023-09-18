package tester;

import static org.junit.Assert.*;
import org.junit.Test;
import student.StudentArrayDeque;
import edu.princeton.cs.introcs.StdRandom;
import java.util.List;
import java.util.ArrayList;

public class TestArrayDequeEC {
    private String generateMsg(List<String> failSeq) {
        StringBuilder result = new StringBuilder();
        for (String elem : failSeq) {
            result.append(elem);
            result.append("\n");
        }
        return result.toString();
    }

    @Test
    public void randomTest() {
        StudentArrayDeque<Integer> stu_deque = new StudentArrayDeque<>();
        ArrayDequeSolution<Integer> solu_deque = new ArrayDequeSolution<>();
        List<String> failSeq = new ArrayList<>();
        int n = 100;
        for (int i = 0; i < n; ++i) {
            int randNum = StdRandom.uniform(0, 4);
            switch (randNum) {
                case 0:
                    failSeq.add("addFirst(" + i + ")");
                    stu_deque.addFirst(i);
                    solu_deque.addFirst(i);
                    assertEquals(generateMsg(failSeq), solu_deque.get(0), stu_deque.get(0));
                    break;
                case 1:
                    failSeq.add("addLast(" + i + ")");
                    stu_deque.addLast(i);
                    solu_deque.addLast(i);
                    assertEquals(generateMsg(failSeq), solu_deque.get(solu_deque.size() - 1), stu_deque.get(stu_deque.size() - 1));
                    break;
                case 2:
                    if (solu_deque.size() > 0) {
                        failSeq.add("removeLast()");
                    }
                    assertEquals(generateMsg(failSeq), solu_deque.size() > 0 ? solu_deque.removeLast() : null,
                                 stu_deque.size() > 0 ? stu_deque.removeLast() : null);
                    break;
                case 3:
                    if (solu_deque.size() > 0) {
                        failSeq.add("removeFirst()");
                    }
                    assertEquals(generateMsg(failSeq), solu_deque.size() > 0 ? solu_deque.removeFirst() : null,
                                 stu_deque.size() > 0 ? stu_deque.removeFirst() : null);
                    break;
                /*case 4:
                    assertEquals(stu_deque.size() > 0 ? stu_deque.get(0) : null,
                            solu_deque.size() > 0 ? solu_deque.get(0) : null);
                    break;
                case 5:
                    assertEquals(stu_deque.size(), solu_deque.size());
                    break;
                case 6:
                    assertEquals(solu_deque.isEmpty(), stu_deque.isEmpty());
                    break;*/
            }
        }
    }
}
