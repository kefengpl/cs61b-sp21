package deque;

import java.util.Comparator;

public class MaxArrayDeque<T> extends ArrayDeque<T> {
    private final Comparator<T> comparator;
    public MaxArrayDeque(Comparator<T> c) {
        super(); // 调用父类的构造函数
        comparator = c;
    }
    public T max() {
        if (isEmpty()) {
            return null;
        }
        T maxVal = get(0);
        for (int i = 1; i < size(); ++i) {
            T thisVal = get(i);
            if (comparator.compare(maxVal, thisVal) < 0) {
                maxVal = thisVal;
            }
        }
        return maxVal;
    }

    public T max(Comparator<T> c) {
        if (isEmpty()) {
            return null;
        }
        T maxVal = get(0);
        for (int i = 1; i < size(); ++i) {
            T thisVal = get(i);
            if (c.compare(maxVal, thisVal) < 0) {
                maxVal = thisVal;
            }
        }
        return maxVal;
    }
}
