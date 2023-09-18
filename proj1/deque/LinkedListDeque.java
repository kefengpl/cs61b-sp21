package deque;

import java.util.Iterator;

/**
 * @apiNote 实现基于双向链表的双端队列
 * 该链表会带有一个头部结点
 * */
public class LinkedListDeque<T> implements Iterable<T>, Deque<T> {
    /**
     * @apiNote 实现结点类型，由于Node类不需要访问外部的双端队列为了节约内存，
     * 可以使用static关键字但是为了公用T泛型类别，这里不适用static关键字
     * */
    private class Node {
        T elem;
        Node last; // 指针指向该节点的上一个结点
        Node next; // 指针指向该结点的下一个结点
        Node(T val) {
            elem = val;
            last = null;
            next = null;
        }
    }

    private final Node head; // 表示头结点(它是固定的，因为有单独的头结点)
    private Node tail; // 表示尾部结点
    private int size = 0; // 表示该双端队列元素的个数，(不包括头结点)

    /**
     * 创建一个空的双端队列，即只创建头部结点
     * 需要保持循环结构：即①head结点的last指向tail
     * ②并且tail结点的next指向head
     * */
    public LinkedListDeque() {
        head = new Node(null);
        tail = head;
        tail.next = head;
        head.last = tail;
    }

    /**
     * 向双端队列的头部插入元素(即向头结点后面插入元素)
     * 如果size == 0，那么需要更新tail指针的位置
     * 只要tail有更新，那么head.last就需要指向最新的tail
     * */
    public void addFirst(T item) {
        Node newNode = new Node(item);
        newNode.last = head;
        newNode.next = head.next;
        head.next = newNode;
        newNode.next.last = newNode; // BUG修复：忘记将原来的第一个数值结点的last指向新结点
        if (size++ == 0) {
            tail = newNode;
            head.last = tail;
        }
    }

    /**
     * 向双端队列的尾部插入元素
     * */
    public void addLast(T item) {
        Node newNode = new Node(item);
        newNode.last = tail;
        tail.next = newNode;
        tail = newNode;
        head.last = tail;
        tail.next = head;
        ++size;
    }

    public int size() {
        return size;
    }

    public void printDeque() {
        Node iter = head.next;
        for (int i = 0; i < size; ++i) {
            System.out.print(iter.elem + " ");
            iter = iter.next;
        }
        System.out.println();
    }

    /**
     * 删除第一个结点，并返回其中的元素，如果没有，就返回null
     * @return 被删除的第一个元素的结点
     * */
    public T removeFirst() {
        if (isEmpty()) {
            return null;
        }
        // 疑惑：这里的removedVal指向结点数值，那么结点还能被销毁吗？
        T removedVal = head.next.elem;
        head.next = head.next.next;
        if (size > 1) {
            // BUG1: 由于上边已经修改了 head.next = head.next.next;
            // 所以不能再写 head.next.next.last = head，这显然是错误的
            head.next.last = head;
        }
        if (size == 1) {
            tail = head;
            head.next = tail;
        }
        --size;
        return removedVal;
    }

    public T removeLast() {
        if (isEmpty()) {
            return null;
        }
        T removedVal = tail.elem;
        tail = tail.last;
        tail.next = head;
        head.last = tail;
        --size;
        return removedVal;
    }

    public T get(int index) {
        if (isEmpty()) {
            return null;
        }
        if (index < 0 || index >= size) {
            return null;
        }
        Node iter = head.next;
        for (int i = 0; i < index; ++i) {
            iter = iter.next;
        }
        return iter.elem;
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

    /**
     * 递归地获取index里面的元素
     * @param curHead 当前的“头结点”，最初是本双端队列的头部结点
     * @param index 相对于当前“头结点”的index，一般而言，头结点下一个Node idx是0
     * */
    private T recursive(int index, Node curHead) {
        if (index == 0) {
            return curHead.next.elem;
        }
        return recursive(index - 1, curHead.next);
    }

    /**
     * wrapper function，里面包含了一个递归实现
     * */
    public T getRecursive(int index) {
        if (isEmpty()) {
            return null;
        }
        if (index < 0 || index >= size) {
            return null;
        }
        return recursive(index, head);
    }
}
