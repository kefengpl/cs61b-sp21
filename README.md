# cs61b-sp21
CS61B for Spring2021

## proj0

根据代码实现游戏2048的一些操作逻辑

唯一需要注意之处在于：(isNull || isEmpty()) 这类判断语句，编译器会简化执行，如果isNull判定为真，那么这个语句的结果就已经决定了，条件判断isEmpty()不会再执行了。所以如果isEmpty()执行了一些重要操作，就一定要把上述语句分开来写

## proj1

主要任务：

    (1)分别基于双向链表和array实现双端队列：核心方法：addFirst, addLast, removeFirst, removeLast 

    (2)基于ArrayDeque实现MaxArrayDeque 

    (3)写随机测试文件 

    (4)Guitar Hero：基于(1)实现的数据结构实现一些算法

(1) 分别基于双向链表和array实现双端队列 

①双向链表：设置一个单独头结点head，tail指针始终指向链表的最后一个结点，例如：如果只有一个单独的head结点，tail指针指向head。这其中的不变量包括：```head->last = tail; tail->next = head```。其余就是正常的链表插入、删除操作。

**BUG**出现在removeFirst函数里面，删除首个结点后，没有将新的首个结点的last指针指向head。

②array：使用“队列”的思想，利用双指针front和tail，其中front指向队列的第一个元素，tail指向最后一个元素的下一个位置，即tail指向队列尾部即将插入的位置。```moveCycle```函数封装了front和tail在将array当成环时向左侧或者右侧移动的逻辑。动态维护数组大小，如果数组空载率大于75%，就使数组大小收缩一半，如果数组满了，就使数组大小扩张为原来的1.25倍。

**BUG**出现在```removeLast```的```handleLowLoad```: 由于```removeLast```已经执行了```--size```，所以handleLowLoad写成```i < size - 1```会删去两个元素，这会导致偶尔的错误。

③重载equals：照着先前代码的判断逻辑即可，注意为了使得元素及顺序相等的LinkListDeque 和 ArrayDeque判断为真，对于Object o， o instanceof Deque即可将二者的类别都判断成Deque，然后直接使用API判断即可。

④重载for each遍历：```implements Iterable<T>```； 实现```private class DequeIterator implements Iterator<T>``` ，包含```构造函数，hasNext，Next```；实现    ```public Iterator<T> iterator() {return new DequeIterator();}```

(2) 基于ArrayDeque实现MaxArrayDeque 

使用传统打擂台算法筛选最大值即可，值得注意的是这类重载了函数指针Comparator。

调用父类的构造函数直接使用```super()```即可。为了重载Comparator，需要设置成员变量```private final Comparator<T> comparator```，在进行比较的时候不使用传统的> <号，而使用```comparator.compare(maxVal, thisVal) < 0```这种比较规则：第一个参数如果小于第二个参数，返回-1；第一个参数等于第二个参数，返回0；第一个参数大于第二个参数，返回+1。

```Comparator<T>```如何构造？可以在测试脚本中使用``` public static class IntegerComparator implements Comparator<Integer>```来实现```public int compare(Integer x, Integer y)``` 函数，然后```Comparator<Integer> c = new IntegerComparator();```即可构造出```comparator<T>```。

(3) 随机测试文件

使用正确的数组和我们实现的数组的结果进行对比即可。使用均匀分布随机数uniform 0---n来实现随机调用方法，对于```addFirst(x)```这种没有返回值的方法，可以使用```assertEquals(generateMsg(failSeq), solu_deque.get(0), stu_deque.get(0))```来判断插入是否成功; 使用List等储存函数调用的String，即可在出错时将List的元素按行打印，得到发生错误过程的函数调用信息。


