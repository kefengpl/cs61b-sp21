package bstmap;

import afu.org.checkerframework.checker.oigj.qual.O;
import org.antlr.v4.runtime.misc.NotNull;

import java.util.*;

/**
 * @Author 3590
 * @Date 2024/1/22 19:56
 * @Description CS61B LAB7 实现 BSTMap。显然，key -- value 结构是以一个 Node 存储的
 */
public class BSTMap<K extends Comparable, V> implements Map61B<K, V> {
    /**
     * Inner Type Node
     * */
    private class Node {
        K key;
        V value;
        Node left;
        Node right;
        Node(K key, V value, Node left, Node right) {
            this.key = key;
            this.value = value;
            this.left = left;
            this.right = right;
        }
    }

    Node root = null; // 初始化根结点
    Integer size = 0; // 该 Map 有多少个结点？

    void swapNodeContent(Node node1, Node node2) {
        K tempKey = node1.key;
        node1.key = node2.key;
        node2.key = tempKey;

        V tempValue = node1.value;
        node1.value = node2.value;
        node2.value = tempValue;
    }

    @Override
    public void clear() {
        root = null;
        size = 0;
    }

    /**
     * 递归查找是否包含某个 key
     * @return Node 查找到的结点本身的引用。如果不存在，就返回 null
     * */
    Node findKey(Node root, K key) {
        if (root == null) {
            return null;
        }
        int compareResult = key.compareTo(root.key);
        if (compareResult == 0) {
            return root;
        } else if (compareResult > 0) {
            return findKey(root.right, key);
        } else {
            return findKey(root.left, key);
        }
    }

    @Override
    public boolean containsKey(K key) {
        return findKey(root, key) != null;
    }

    @Override
    public V get(K key) {
        Node node = findKey(root, key);
        return node == null ? null : node.value;
    }

    @Override
    public int size() {
        return size;
    }

    /**
     * 这里我们假设不允许有重复结点插入！
     * */
    @Override
    public void put(K key, V value) {
        if (root == null) {
            root = new Node(key, value, null, null);
            ++size;
            return;
        }
        Node pointer = root;
        while (pointer != null) {
            int compareResult = key.compareTo(pointer.key);
            if (compareResult == 0) {
                return; // 如果有相同元素，则直接返回，禁止更新
            } else if (compareResult > 0) {
                if (pointer.right == null) {
                    pointer.right = new Node(key, value, null, null);
                    ++size;
                    return;
                } else {
                    pointer = pointer.right;
                }
            } else {
                if (pointer.left == null) {
                    pointer.left = new Node(key, value, null, null);
                    ++size;
                    return;
                } else {
                    pointer = pointer.left;
                }
            }
        }
    }

    @Override
    public Set<K> keySet() {
        Set<K> resultSet = new TreeSet<>();
        for (K key : this) {
            resultSet.add(key);
        }
        return resultSet;
    }

    /**
     * 获取某个结点的直接后继
     * 就是它右子树一路向左的结点
     * */
    Node nextNodeOf(Node root) {
        if (root == null) {
            return null;
        }
        if (root.right == null) {
            return null;
        }
        Node curNode = root.right;
        while (curNode.left != null) {
            curNode = curNode.left;
        }
        return curNode;
    }

    /**
     * 递归实现删除结点，返回删除结点后的根结点
     * 假设 key 一定存在且符合要求，因为调用它的函数会检查这个事情
     * */
    Node innerRemove(Node root, K key) {
        if (root == null) {
            return null;
        }
        int compareResult = key.compareTo(root.key);
        if (compareResult < 0) {
            root.left = innerRemove(root.left, key);
            return root;
        } else if (compareResult > 0) {
            root.right = innerRemove(root.right, key);
            return root;
        } else { // 最复杂的情况：删除的结点是根结点
            if (root.left == null && root.right == null) {
                return null;
            } else if (root.left != null && root.right != null) {
                Node nextNode = nextNodeOf(root);
                swapNodeContent(root, nextNode);
                root.right = innerRemove(root.right, key);
                return root;
            } else {
                return root.left == null ? root.right : root.left;
            }
        }
    }

    @Override
    public V remove(K key) {
        Node node = findKey(root, key);
        if (node == null) {
            return null;
        }
        root = innerRemove(root, key);
        --size;
        return node.value;
    }

    @Override
    public V remove(K key, V value) {
        Node node = findKey(root, key);
        if (node == null || !node.value.equals(value)) {
            return null;
        }
        root = innerRemove(root, key);
        --size;
        return node.value;
    }

    /**
     * 我们假设以递增序列进行迭代。实现策略：非递归中序遍历(用到栈)
     * */
    class BSTMapIterator implements Iterator<K> {
        private Stack<Node> stack = new Stack<>();

        /**
         * 以 root 出发，一路向左入栈元素
         * */
        void pushLeftNode(Node root) {
            Node node = root;
            while (node != null) {
                stack.push(node);
                node = node.left;
            }
        }

        /**
         * 中序遍历的一个步骤：一路向左将所有元素入栈
         * */
        BSTMapIterator() {
            pushLeftNode(root);
        }

        @Override
        public boolean hasNext() {
            return !stack.empty();
        }

        @Override
        public K next() {
            Node node = stack.pop();
            if (node.right != null) {
                pushLeftNode(node.right);
            }
            return node.key;
        }
    }

    /**
     * 需要返回的是一个 key 的迭代器，方便遍历 key
     * */
    @Override
    public Iterator<K> iterator() {
        return new BSTMapIterator();
    }
}
