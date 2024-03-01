# GitLet 设计文档
- 实现一个简化版本的 git。由于代码编译中有中文注释会报错，故代码中的所有说明皆以英文来写。
- 网上博客对于 git 设计的理解未必正确，如果希望更好地理解 git，最好的方式还是查阅 git 官方文档。
- 这是UC Berkeley CS61B的一个课程项目，项目说明文档：https://sp21.datastructur.es/materials/proj/proj2/proj2#global-log
- 此外，为了设计存储方式和存储的文件结构，还需要参考 git 官方文档：https://www.progit.cn/#_plumbing_porcelain

# 主要工具类
## Class: Main
- 通过命令行传入 Main(String...args) 中的args。在调用具体命令对应的方法之前，该类会对参数个数是否符合要求和仓库是否初始化进行检查。
- 分支很多时，为了减少编程错误，最好使用反射，因为 switch 分支忘记写 break 的概率极高，项目运行时总会带来意外之喜。但是本课程的教授似乎对反射嗤之以鼻。
- 因此，这里使用Java8新特性：方法引用，以实现每个命令的检查、调用与传参，这样做可以减少调用和传参时发生的错误。 
- 每个命令具体实现在 Repository.java 中。
- 神奇的是，Runnable 在 Java8 中可以当作 Function，它不接受参数，返回是 void。此时，它并不应用于多线程，而是函数接口。

## Class: Repository
**变量**
- HEAD: 维护了 HEAD 指针，与 .gitlet/HEAD 关联。在程序启动时，如果已经初始化，就从 .gitlet 中读取 HEAD 文件，
  HEAD 文件存放的是分支名。比如 HEAD = master。

**功能**
- 实现了各类命令的调用方法。比如，命令行中输入 add file.txt，那么会调用其中的 add() 函数。

## Class: IndexUtils
用于处理暂存区保存到磁盘，以及写入暂存区的操作。

**变量**
- indexMap: 静态变量，存放 文件名称-->版本(sha1) 的映射关系，与 .gitlet/index 文件直接关联。它代表下一次要提交的 map(file --> version)。
- stagedFileContents: 静态变量，存放 版本(sha1)-->文件具体内容，与 .gitlet/staged-files 文件直接关联，每次提交时会生成 objects 文件夹的文件对象，并清空 stagedFileContents 对应的文件及其本身。

**方法**
- stageFile(String fileName): 将工作区的一个文件计算sha1，分别存储到 indexMap 和 stagedFileContents 里，但是不会写入文件。
- unstageFile(String fileName): 将工作区的一个文件移出 indexMap 和 stagedFileContents，也不会对文件进行任何操作。该设计存在些许问题：
如果一个人 stageFile 了两次，每次都是不同版本，而 indexMap 中只能保留一个记录(只因它们的文件名相同)；当你 unstageFile 的时候，会清除 indexMap
中该文件名为 key 的记录 

**使用**
- 对于 indexMap 和 stagedFileContents 两个内存变量的修改，需要通过 saveIndex() 方法持久到内存中。

# 功能实现思路
## init
初始化命令，类似于 gitlet init。它做的事情如下：
1. 在当前工作目录创建 .gitlet 文件夹
2. 在.gitlet中创建 INDEX(index)，HEAD，STAGED_FILE(staged-files)
3. 创建文件夹：commits(文件名是commit的id，内容是Commit对象的内容), objects(不同版本的文件对象), branches(文件名是 branch 的名称，内容是 branch 指向的 commitId)
4. 进行一次初始空提交：生成 commit 对象并把该对象保存到 .gitlet/commits 文件夹中，文件名是 commit-id。
5. 在 .gitlet/branches 中创建 master 分支的文件，文件名 master，存储内容：master 对应的 commit-id。

## add
将文件添加到暂存区，它做的事情如下。
注意：每次提交完毕后，indexMap 与 该 commit 的 fileVersionMap 是完全一致的，stagedFileContents是被清空的。

1. 检测 indexMap 中是否有该 (文件名-->sha1)，如果有，表明用户没有对文件做出改变，直接返回即可。
2. 否则向 indexMap 添加一个条目， (文件名-->sha1)，同时向 stagedFileContents 添加 (sha1-->文件具体内容)；随后持久化到 index 和 staged-files 文件里面。

## commit
进行一次提交。提示：对于 untracked file，git 在提交时会忽略它们，并且允许提交。
1. 如果 indexMap 和当前提交(进行本次提交之前的提交)的 fileVersionMap 是一致的，则不提交，因为没有改变。
2. 生成 commit 对象，叫做 newCommit；当前 commit 记录为 oldCommit
3. 随后将 oldCommit 和 newCommit 的 fileVersionMap 进行对比，将一些文件从 stagedFileContents 持久化到 .gitlet/objects 中。这种做法的缺陷在于，
由于仅和当前 commit 进行对比， 如果 commit1 commit3 包含相同版本的 hello.txt(v1)，commit2 包含不同版本的 hello.txt(v2)，在提交 commit3 时，会导致 v1 
版本的文件被重复写入一次。当然，这不会带来错误。
4. 清空 stagedFileContents 对应的文件 staged-files。显然， stagedFileContents 作为内存变量，本次命令结束后会自动释放内存。
5. 将当前 branch，比如 master 的指针指向新的 commit id

## rm
- 直观上，该命令移除暂存区的对应文件。它的思想是这样的：对于被当前提交跟踪的文件，用户应该先在 CWD 删除文件，再调用 rm() 在暂存区标记为删除(add 的反向操作)。
  因为 rm() 会记录下一次提交时，哪些文件会被添加，哪些文件会被删除。所以下面的执行逻辑中，如果用户没有在 CWD 删除文件，就帮他删除位于 CWD 的文件。
- 对于当前提交未跟踪的文件，属于用户新添加的文件，只进行暂存区级别的操作(提供 add 的反向操作)，不对 CWD 进行操作，它的状态只有两种 === Untracked Files === / === Staged Files ===。
- 核心：只有 tracked files 才会在 status() 中被标记为 === Removed files ===，表示将要在下一次提交中移除的文件，
  恰好是 添加 CWD 文件 --> add() --> commit() 的反向操作：删除 CWD 文件 --> rm() --> 。
核心功能依然是移除暂存区中的文件(或者将某文件在暂存区标记为移除)，移除规则如下：
1. 如果一个文件没有被暂存区和commit跟踪(在 commit 的 fileVersionMap 中)，报错，没有移除文件的必要。
2. 如果一个文件被暂存区跟踪，就将它移除暂存区，注意，这也需要保存，调用saveIndex().
3. 如果文件被 commit 跟踪，就在工作区中删除该文件。

## checkout (file)
- checkout -- fileName: 恢复 head commit 储存的文件到工作区
- checkout [commit id] -- [file name]: 恢复某次 commit 对应的 file 到工作区
- 提示，这两个命令不会修改暂存区！

## checkout (branch)

## find
它可以打印出 符合某个 commit message 的 commit 的所有 id。实现起来很容易，
直接去 .gitlet/commits 中逐个读取文件到内存然后分析其 commit message 即可。

## branch
创建分支，它需要做的是：在.gitlet/branches中创建一个新文件，文件名是 分支名，文件内容是head指针当前指向分支的对应的 commitId(HEAD commit id)。

## 项目难点：merge
- 分支的主要作用在于：二并一，并且它只会改变当前 HEAD 对应分支指向的 commit，不会改变 merge 命令后面那个分支的指针指向。
- 应该对 merge 有一个直观的理解，而不是机械执行 gitlet 说明文档的复杂逻辑，结合下面的直观理解，可以简化代码实现，而不是分类讨论①~⑧。
- 非冲突合并的直观理解1：对于某个文件，如果一方相对于公共祖先结点进行了添加、删除、修改，而另一方相对于公共祖先结点不变，则合并后生成的提交需要跟踪单方所做的修改。
- 非冲突合并的直观理解2：对于某个文件，如果一方相对于公共祖先结点进行了添加、删除、修改，而另一方相对于公共祖先结点进行了添加、删除、修改，
                     并且两个分支所做的修改完全相同(比如都删除了文件，或者修改后文件内容相同)，则不会产生合并冲突，维持现状即可。
- 冲突合并的直观理解：相对于公共祖先结点，如果一方对某个文件进行了添加、删除、修改，而另一方对该文件也做了添加、删除、修改，并且两个分支对该文件的改变相对于祖先结点而言不一致，就会带来合并冲突。
- 如何处理 CWD(当前工作目录) ？ 需要意识到，merge 的本质也是产生一次新的提交，为此，它需要先改变 CWD，然后将改变添加到 暂存区，最后才进行提交。
- 至少应该先通过集合运算，确定哪些文件在三个 commit 结点都有，哪些是其中二者拥有的，哪些仅其中一者拥有，这样会使得问题变得更清晰。

**特殊情况，fast-forward**
- commit1 --- commit2 --- commit3 --- commit4
                        master     hot-fix
- git merge hotfix, 比如当前在 master 分支上，那么此时会将 master 指针移动到 commit4， 这被称作 fast-forward 
- 反过来，如果你在 hot-fix 分支上运行 git merge master, 那么此时由于 hot-fix 就是新版本，所以无需移动任何指针。

**一般情况的逻辑**
假设两个分支分叉，那么分叉点就是公共祖先结点
- ① 对于一个文件，与公共祖先结点相比；如果当前分支没有修改，而目标分支修改了 --> 应该先将目标分支版本的文件加入CWD，然后添加暂存区 
- ② 对于一个文件，与公共祖先结点相比；如果当前分支修改了，而目标分支没有修改 --> (对于当前分支而言)保持原样 
- ③ 如果两个分支对于同一个文件的修改规则相同，比如都删除了或者内容相同；-->  (对于当前分支而言)保持原样 
     补充：如果两个分支都删除了某个文件，但是当前 CWD 中有一个同名文件，不处理它 (不跟踪，也不缓存) 
- ④ 没出现在公共祖先结点的文件，且仅出现在当前分支 --> (对于当前分支而言)保持原样 
- ⑤ 没出现在公共祖先结点的文件，且仅出现在目标分支 --> 应该取出到 CWD, 并添加暂存区 
- ⑥ 出现在公共祖先结点的文件，当前分支未修改，目标分支该文件缺失 --> 从 CWD 移除 并 取消跟踪该文件 
- ⑦ 出现在公共祖先结点的文件，目标分支未修改，当前分支该文件缺失 --> (对于当前分支而言)保持原样(remain absent.) 
- ⑧ a.如果对于一个文件，都相对于公共祖先结点修改了，并且修改的内容不同。 / b.相对于祖先结点，一方修改了文件，一方删除了文件。 / c.公共祖先结点文件缺失，两个分支的内容不同。 
     在上述情况下，需要处理合并冲突，需要给CWD的冲突文件写入 冲突处理内容；然后将该文件暂存。如果某分支的文件删除，把它当作空文件。

- 在上述复杂情况完成后，merge 自动提交，log 信息 Merged [given branch name] into [current branch name]. (注意：log中需要打印)
- 如果遇到合并冲突，前面的提交也会发生(注意：这与 git 不同)，打印 Encountered a merge conflict.
- 合并的提交有两个 parent，第一个 parent 就是当前分支的 parent，另一个是目标分支的 parent

**merge()函数的实现：**
- 这里将两个分支尽头的提交分别叫做：当前分支，目标分支。虽然说是分支，但是本质上是提交。merge 函数围绕 当前分支(提交)，目标分支(提交)，公共祖先(提交)这三个提交展开。
- 使用图的DFS以及结点的入度求解两个分支的公共祖先提交。
- 如果公共祖先是目标分支和当前分支之一，则说明目标分支和当前分支在一条线上。如果当前分支是公共祖先，则移动分支指针到目标分支所在提交，然后切换到目标分支所在
的提交即可；如果目标分支是公共祖先，说明已经合并完成，无需进行其它操作。
- 对于其它需要合并的情况，需要分为多类讨论。定义概念：文件一致性。它指的是对于一个文件，如果A提交和B提交都没有跟踪该文件，或者两个提交都跟踪了该文件
且内容完全一致，则文件对于A和B两个提交而言是一致的。一致代表没有修改该文件。下面的讨论按照该一致性定义展开。
- 对于某个文件，如果：
- ①公共祖先和当前分支一致，公共祖先和目标分支不一致，则该文件需要与目标分支的状态一致。
- ②公共祖先和目标分支一致，公共祖先和当前分支不一致，则该文件需要与当前分支的状态一致，即什么也不用做。
- ③公共祖先和目标分支、当前分支都不一致，但是目标分支和当前分支一致，则该文件与当前或目标分支的状态一致，即什么也不用做。
- ④公共祖先、目标分支、当前分支三者皆不一致，则合并冲突，此时将目标分支和当前分支的文件拼合在一起，如果一方没有这文件，按照空文件处理。

# bug 记录区
- Main中的问题：switch 分支不加 break 会带来各种惊奇的错误
- String 是可变对象，如果使用 s = s + "a"，这将重新分配堆空间。所以 List<String> forEach(s -> s = s + "a"); 不会改变原有数组的内容。
- merge 中，getSplitCommit 出错，是因为 commitTraceBack 返回的 List<Commit> 顺序是 由 新 --> 旧。寻找公共祖先，需要把该 List 逆置。
- merge 中，if (!branchCommitFiles.contains(fileName)) 中的 branchCommitFiles 写成了 splitPointFiles。应该检测目标分支是否删除了该文件
- 下面代码的正确逻辑如下。最开始写反了，导致 bug
```angular2html
if (FileUtils.isOverwritingOrDeletingCWDUntracked(fileName, currentCommit)) { 
    System.out.println(MERGE_MODIFY_UNTRACKED_WARNING);
    return;
} else {
    rm(fileName);
}
```
- merge 最后生成的 commit 在 setSecondParentId 之后必须保存到 .gitlet/commits 中，否则会导致 log 输出错误
- merge 最后生成了新的 commit，所以必须将所在 branch 的指针指向该 合并 commit 的 commit id。
- 算法问题：在一个图(Graph)中寻找公共祖先。在下面这张图中，两个红色结点的公共祖先是左下角的结点，而不是最左侧的结点。这是一个有向无环图。
该问题的解决思路是：从两个分支提交结点出发，分别对有向图进行深度优先遍历，从而获得所有祖先提交(这里定义祖先包括这个提交自身)。随后对两个结点获得的
祖先集合取交集，得到公共祖先。公共祖先之间也构成了有向图，每个公共祖先提交是有向图中的一个结点。最小公共祖先的定义是，它不是任何一个公共祖先的祖先。
得到最小公共祖先，问题转化为求有向图入度为0的结点。每个结点的入度可以通过遍历图所有结点的出度指向获取。于是，在引入分支与合并后，提交结构由原来的
链表结构转化为了图结构。
![img.png](img.png)


# 其它知识
**知识补充：不可变对象**

不可变类型（Immutable type）是指一旦创建了该类型的对象，其状态（即对象中的数据）就不能被改变。
在Java中，不可变对象的内容一旦被创建后就不能被修改，任何修改都会导致创建一个新的对象。
Java中除了String外，Integer、Float、BigInteger等包装类也是不可变的。

**keySet取并集**

如果希望修改获得的keySet，需要 new HashMap(map.keySet())。重新分配一块堆空间。因为祖宗之法不可变，Map中取出的keySet()是一个引用，
在不修改map结构的情况下，这个引用的keySet()是不可修改的。

**Map的Val更新**

java中map的val更新是啰嗦的，下面这个语句实现了类似C++中 _map[key]++ 的效果，map.put(key, map.getOrDefault(key, 0) + 1);