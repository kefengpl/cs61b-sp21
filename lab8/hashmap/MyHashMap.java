package hashmap;

import java.util.*;

/**
 *  A hash table-backed Map implementation. Provides amortized constant time
 *  access to elements via get(), remove(), and put() in the best case.
 *
 *  Assumes null keys will never be inserted, and does not resize down upon remove().
 *  @author YOUR NAME HERE
 */
public class MyHashMap<K, V> implements Map61B<K, V> {
    /**
     * Protected helper class to store key/value pairs
     * The protected qualifier allows subclass access
     */
    protected class Node {
        K key;
        V value;

        Node(K k, V v) {
            key = k;
            value = v;
        }
    }

    /* Instance Variables 盛放每个哈希桶的数组。比如链地址法索引0的地方构成一个链表，那么该链表就是一个桶(bucket) */
    private Collection<Node>[] buckets;
    private HashSet<K> keySet = new HashSet<>(); // holds all of the keys
    private int initialSize = 16;
    private double loadFactor = 0.75;

    /** Constructors */
    public MyHashMap() {
        // JAVA DEFAULT ： 数组不能使用泛型，所以需要强制转换
        buckets = createTable(this.initialSize);
    }

    public MyHashMap(int initialSize) {
        this.initialSize = initialSize;
        buckets = createTable(this.initialSize);
    }

    /**
     * MyHashMap constructor that creates a backing array of initialSize.
     * The load factor (# items / # buckets) should always be <= loadFactor
     *
     * @param initialSize initial size of backing array
     * @param maxLoad maximum load factor
     */
    public MyHashMap(int initialSize, double maxLoad) {
        this.initialSize = initialSize;
        this.loadFactor = maxLoad;
        buckets = createTable(this.initialSize);
    }

    /**
     * Returns a new node to be placed in a hash table bucket
     */
    private Node createNode(K key, V value) {
        return new Node(key, value);
    }

    /**
     * Returns a data structure to be a hash table bucket
     *
     * The only requirements of a hash table bucket are that we can:
     *  1. Insert items (`add` method)
     *  2. Remove items (`remove` method)
     *  3. Iterate through items (`iterator` method)
     *
     * Each of these methods is supported by java.util.Collection,
     * Most data structures in Java inherit from Collection, so we
     * can use almost any data structure as our buckets.
     *
     * Override this method to use different data structures as
     * the underlying bucket type
     *
     * BE SURE TO CALL THIS FACTORY METHOD INSTEAD OF CREATING YOUR
     * OWN BUCKET DATA STRUCTURES WITH THE NEW OPERATOR!
     *
     * @note 这里随意选择一个bucket实体类即可。子类会重载该方法
     */
    protected Collection<Node> createBucket() {
        return new HashSet<>();
    }

    /**
     * Returns a table to back our hash table. As per the comment
     * above, this table can be an array of Collection objects
     *
     * BE SURE TO CALL THIS FACTORY METHOD WHEN CREATING A TABLE SO
     * THAT ALL BUCKET TYPES ARE OF JAVA.UTIL.COLLECTION
     *
     * @param tableSize the size of the table to create
     */
    private Collection<Node>[] createTable(int tableSize) {
        return new Collection[tableSize];
    }

    /**
     * 返回一个对象的哈希索引，具体实现如下[处理方法来自Algorithms，将32位的有符号数变为31位的无符号数]
     * 此外，PPT中的方法是 floorMod(-1, 5) 即可返回正值
     * @bug 潜在的严重错误：容器扩容后会导致哈希值前后是不一致的，由于取模运算。
     * @return 映射后的索引值
     * */
    private int hashing(K key) {
        int hashCode = key.hashCode();
        return (hashCode & 0x7fffffff) & (this.buckets.length - 1);
    }

    @Override
    public void clear() {
        // 提示：快速清空数组的办法，当然，它也是用 for (int i = 0...) 这种循环完成的
        Arrays.fill(buckets, null);
        keySet.clear();
    }

    @Override
    public boolean containsKey(K key) {
        return keySet.contains(key);
    }

    @Override
    public V get(K key) {
        if (!containsKey(key)) {
            return null;
        }
        int index = hashing(key) % buckets.length;
        for (Node node : buckets[index]) {
            if (node.key.equals(key)) {
                return node.value;
            }
        }
        return null;
    }

    @Override
    public int size() {
        return keySet.size();
    }

    /**
     * 检查是否需要扩大数组大小，扩容因子设为 2
     * 再提示：扩容的时候需要存入新的 HASH 值，
     * 在源码中，扩容因子是2，并且初始大小等都是2的幂，扩容负载是0.75。它会rehashing所有结点
     * 官方解释为什么直接拆成两个链表
     * because we are using power-of-two expansion,
     * the elements from each bin must either stay at same index, or move with a power of two offset in the new table.
     * 具体操作：如果原始 table 中 idx 处指向一个链表，那么它会把一个链表拆分为两部分[它两个链表都使用尾插法，且维护了head和tail两个指针]
     * 分别储存在 newTable[idx] 和 newTable[idx + 原始table容量] 之中.
     *
     * 注意：源码中 n = table.length; (n - 1) & (hash = hash(key)) 等效于 hash % n 。
     * 也就是说，HashMap 的哈希函数是十分朴素的对容量取模。所以它会遇到和这里相同的问题：即当table扩容后，哈希值会改变
     *
     * 当链表长度 > 8 之后，这个桶就会从链表转为 红黑树。
     *
     * 相对于普通HashMap，Java做了哪些性能优化？
     * ① 当链表长度 > 8 之后，这个桶就会从链表转为 红黑树。
     * ② hash 运算，替代了原始的取模运算，取而代之的是位运算。
     * ③ resize 时，将扩容后，每个原始表一分为二，提高效率
     *
     * 补充：HashMap 不是线程安全的
     * */
    private void expandBucketsSize() {
        int bucketNumber = 0;
        for (var bucket : buckets) {
            if (bucket != null) {
                bucketNumber += 1;
            }
        }
        double factorLoad = (double) bucketNumber / buckets.length;
        if (factorLoad <= loadFactor) {
            return;
        }
        Collection<Node>[] newBuckets = createTable(buckets.length * 2);
        Collection<Node>[] oldBuckets = this.buckets;
        buckets = newBuckets;
        // 应该遍历旧的 HASH TABLE，对每个元素逐个重新计算 HASH VALUE
        for (Collection<Node> bucket : oldBuckets) {
            if (bucket != null) {
                for (Node node : bucket) {
                    int newIndex = hashing(node.key);
                    if (newBuckets[newIndex] == null) {
                        newBuckets[newIndex] = createBucket();
                    }
                    newBuckets[newIndex].add(node);
                }
            }
        }
    }

    @Override
    public void put(K key, V value) {
        if (containsKey(key)) {
            int index = hashing(key);
            for (Node node : buckets[index]) {
                if (node.key.equals(key)) {
                    node.value = value;
                }
            }
            return;
        }
        Node node = new Node(key, value);
        int index = hashing(key);
        if (buckets[index] == null) {
            buckets[index] = createBucket();
        }
        buckets[index].add(node);
        keySet.add(key);
        expandBucketsSize();
    }

    @Override
    public Set<K> keySet() {
        return this.keySet;
    }

    private V commonRemove(K key, V value) {
        if (!containsKey(key)) {
            return null;
        }
        int index = hashing(key);
        V removeVal = null;
        Collection<Node> bucket = buckets[index];
        for (Node node : bucket) {
            if (node.key.equals(key)) {
                boolean innerCheck = (value == null || (node.value.equals(value)));
                if (innerCheck) {
                    removeVal = node.value;
                    bucket.remove(node);
                    keySet.remove(key);
                }
            }
        }
        if (bucket.isEmpty()) {
            buckets[index] = null;
        }
        return removeVal;
    }

    @Override
    public V remove(K key) {
        return commonRemove(key, null);
    }

    @Override
    public V remove(K key, V value) {
        return commonRemove(key, value);
    }

    @Override
    public Iterator<K> iterator() {
        return this.keySet.iterator();
    }

}
