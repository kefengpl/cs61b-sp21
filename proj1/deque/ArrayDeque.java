package deque;

import java.util.Iterator;

/**
 * @apiNote 实现基于ARRAY的双端队列
 * 使用双指针，就像队列一样来维护数组
 * */
public class ArrayDeque<T> implements Iterable<T>, Deque<T> {
    // 数组的初始大小是8
    private static final int INIT_SIZE = 8;
    // 对于长度>=16的array，其中的元素个数应该>=4，即负载率最低是0.25
    private static final double LOWEST_LOAD = 0.25;
    // 扩充率是1.25，只要数组的元素满了，就给数组扩充成原来的1.25倍
    private static final double EXPAND_RATE = 1.25;
    // 收缩率是0.5，如果数组负载<0.25，就
    private static final double SHRINK_RATE = 0.5;

    private T[] elemArray;
    private int front; // 表示双端队列的队首元素本身，不需要移动位置
    private int tail; // 表示双端队列的队尾的待填充区域，需要向左侧移动1位才能获得队尾元素
    private int size = 0;

    private enum Direction { LEFT, RIGHT }
    private enum DequeLocation { FRONT, TAIL }

    public ArrayDeque() {
        elemArray = (T[]) new Object[INIT_SIZE];
        front = 0;
        tail = 0;
    }
    /**
     * 将有限的数组大小看成一个“环”，将front或者tail向右侧、左侧移动1位
     * */
    private void moveCycle(Direction d, DequeLocation l) {
        if (d == Direction.LEFT && l == DequeLocation.FRONT) {
            front = (front + elemArray.length - 1) % elemArray.length;
        }
        if (d == Direction.RIGHT && l == DequeLocation.FRONT) {
            front = (front + 1) % elemArray.length;
        }
        if (d == Direction.LEFT && l == DequeLocation.TAIL) {
            tail = (tail + elemArray.length - 1) % elemArray.length;
        }
        if (d == Direction.RIGHT && l == DequeLocation.TAIL) {
            tail = (tail + 1) % elemArray.length;
        }
    }
    /**
     * 函数本身不会对变量进行任何修改
     * @return 返回如果进行moveCycle得到的结果
     * */
    private int getMoveCycle(Direction d, DequeLocation l) {
        if (d == Direction.LEFT && l == DequeLocation.FRONT) {
            return (front + elemArray.length - 1) % elemArray.length;
        }
        if (d == Direction.RIGHT && l == DequeLocation.FRONT) {
            return (front + 1) % elemArray.length;
        }
        if (d == Direction.LEFT && l == DequeLocation.TAIL) {
            return (tail + elemArray.length - 1) % elemArray.length;
        }
        if (d == Direction.RIGHT && l == DequeLocation.TAIL) {
            return (tail + 1) % elemArray.length;
        }
        return -1; // -1表示出现了未知的Error
    }

    /**
     * 处理插入数据时数组满了的情况
     * 如果数组满了，需要给数组扩容，并将front置于0，tail置于合适位置
     * */
    private void handleFullArray() {
        T[] newArray = (T[]) new Object[(int) (EXPAND_RATE * elemArray.length)];
        // 将原来的数组按照“环”，从front拷贝到tail(此时front == tail)，直接使用数组大小最合理
        // 拷贝完成后就将front和tail重新放到合适的位置，(新数组还未成环)，此时就让front为0，
        // tail为数组中的最后一个元素的下一个元素
        for (int i = 0; i < elemArray.length; ++i) {
            newArray[i] = elemArray[front];
            moveCycle(Direction.RIGHT, DequeLocation.FRONT);
        }
        front = 0;
        tail = elemArray.length % newArray.length;
        elemArray = newArray;
    }

    /**
     * 向队首添加一个元素
     * */
    public void addFirst(T item) {
        ++size;
        if (size > elemArray.length) {
            handleFullArray();
        } // if 结束后再插入新的元素，size已经在开头增加了，因此无需再处理
        moveCycle(Direction.LEFT, DequeLocation.FRONT);
        elemArray[front] = item;
    }

    /**
     * 向队列尾部添加一个元素
     * */
    public void addLast(T item) {
        ++size;
        if (size > elemArray.length) {
            handleFullArray();
        }
        elemArray[tail] = item;
        moveCycle(Direction.RIGHT, DequeLocation.TAIL);
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public int size() {
        return size;
    }

    public void printDeque() {
        int iter = front;
        for (int i = 0; i < size; ++i) {
            System.out.print(elemArray[iter] + " ");
            iter = (iter + 1) % elemArray.length;
        }
        System.out.println();
    }

    private double getLoadRate() {
        return (double) size / elemArray.length;
    }

    /**
     * 处理数组元素过少的问题，让数组缩小
     */
    private void handleLowLoad(DequeLocation l) {
        T[] newArray = (T[]) new Object[(int) (SHRINK_RATE * elemArray.length)];
        // 表示removeFirst
        if (l == DequeLocation.FRONT) {
            for (int i = 0; i < size; ++i) {
                moveCycle(Direction.RIGHT, DequeLocation.FRONT);
                newArray[i] = elemArray[front];
            }
        }
        // 表示removeLast BUG: 由于removeLast已经执行了--size，所以这里写成i < size - 1会删去两个元素
        if (l == DequeLocation.TAIL) {
            for (int i = 0; i < size; ++i) {
                newArray[i] = elemArray[front];
                moveCycle(Direction.RIGHT, DequeLocation.FRONT);
            }
        }
        front = 0;
        tail = size % newArray.length;
        elemArray = newArray;
    }

    /**
     * 要求array负载率>=25%(在数组长度>=16的情况下)
     * 此时需要在删除元素之前就直接调整数组的大小，然后再删除元素
     * */
    public T removeFirst() {
        if (size == 0) {
            return null;
        }
        --size; // 先不删除元素，但是先将size - 1
        T result = elemArray[front]; // 获取结果
        if (getLoadRate() < LOWEST_LOAD && elemArray.length >= 16) {
            handleLowLoad(DequeLocation.FRONT);
        } else {
            moveCycle(Direction.RIGHT, DequeLocation.FRONT);
        }
        return result;
    }

    public T removeLast() {
        if (size == 0) {
            return null;
        }
        --size; // 先不删除元素，但是先将size - 1
        T result = elemArray[getMoveCycle(Direction.LEFT, DequeLocation.TAIL)]; // 获取结果
        if (getLoadRate() < LOWEST_LOAD && elemArray.length >= 16) {
            handleLowLoad(DequeLocation.TAIL);
        } else {
            moveCycle(Direction.LEFT, DequeLocation.TAIL);
        }
        return result;
    }

    public T get(int index) {
        if (size == 0) {
            return null;
        }
        if (index >= size || index < 0) {
            return null;
        }
        return elemArray[(front + index) % elemArray.length];
    }

    private class DequeIterator implements Iterator<T> {
        private int curPos; // 表示即将访问的索引位置，初始即将访问索引0
        DequeIterator() {
            curPos = 0;
        }

        public boolean hasNext() {
            return curPos < size;
        }

        public T next() {
            return get(curPos++);
        }
    }

    public Iterator<T> iterator() {
        return new DequeIterator();
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null) {
            return false;
        }
        if (!(o instanceof Deque)) {
            return false;
        }
        Deque<T> other = (Deque<T>) o;
        if (other.size() != this.size()) {
            return false;
        }
        for (int i = 0; i < this.size(); ++i) {
            if (!this.get(i).equals(other.get(i))) {
                return false;
            }
        }
        return true;
    }
}
