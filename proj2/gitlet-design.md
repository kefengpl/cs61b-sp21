# GitLet 设计文档

这是UC Berkeley CS61B的一个课程项目，项目说明文档：https://sp21.datastructur.es/materials/proj/proj2/proj2#global-log
实现一个简化版本的.git。由于代码编译中有中文注释会报错，故代码中的所有说明皆以英文来写。

## Main
- 通过命令行传入 Main(String...args) 中的args
- 使用的Java8新特性：方法引用，以实现每个命令的检查、调用与传参，这样做可以减少调用和传参时发生的错误。 
- 每个命令具体实现在 Repository.java 中。事实上，也可以使用反射。
- 神奇的是，Runnable 在 Java8 中可以当作 Function，它不接受参数，返回是 void。此时，它并不应用于多线程，而是函数接口。

## Repository
**变量**
- HEAD: 维护了 HEAD 指针，与 .gitlet/HEAD 关联。在程序启动时，如果已经初始化，就从 .gitlet 中读取 HEAD 文件，HEAD 文件存放的是分支名。比如 HEAD = master。

**功能**
- 实现了各类命令的调用方法

## IndexUtils
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
进行一次提交。
1. 如果 indexMap 和当前提交(进行本次提交之前的提交)的 fileVersionMap 是一致的，则不提交，因为没有改变。
2. 生成 commit 对象，叫做 newCommit；当前 commit 记录为 oldCommit
3. 随后将 oldCommit 和 newCommit 的 fileVersionMap 进行对比，将一些文件从 stagedFileContents 持久化到 .gitlet/objects 中。这种做法的缺陷在于，
由于仅和当前 commit 进行对比， 如果 commit1 commit3 包含相同版本的 hello.txt(v1)，commit2 包含不同版本的 hello.txt(v2)，在提交 commit3 时，会导致 v1 
版本的文件被重复写入一次。当然，这不会带来错误。
4. 清空 stagedFileContents 对应的文件 staged-files。显然， stagedFileContents 作为内存变量，本次命令结束后会自动释放内存。
5. 将当前 branch，比如 master 的指针指向新的 commit id

## rm
移除文件，移除规则如下：
1. 如果一个文件没有被暂存区和commit跟踪(在 commit 的 fileVersionMap 中)，报错，没有移除文件的必要。
2. 如果一个文件被暂存区跟踪，就将它移除暂存区，注意，这也需要保存，调用saveIndex().
3. 如果文件被 commit 跟踪，就在工作区中删除该文件。

## checkout (file)
- checkout -- fileName: 恢复 head commit 储存的文件到工作区
- checkout [commit id] -- [file name]: 恢复某次 commit 对应的 file 到工作区
- 提示，这两个命令不会修改暂存区！

## checkout (branch)

## find
它可以打印出 符合某个 commit message 的 commit 的所有 id

## branch
创建分支，它需要做的是：在.gitlet/branches中创建一个新文件，文件名是 分支名，文件内容是head指针指向原来分支的对应的commitId(HEAD commit id)。

## bug 记录区
- Main中的问题：switch 分支不加 break 会带来各种惊奇的错误
- String 是可变对象，如果使用 s = s + "a"，这将重新分配堆空间。所以 List<String> forEach(s -> s = s + "a"); 不会改变原有数组的内容。

**知识补充：不可变对象**

不可变类型（Immutable type）是指一旦创建了该类型的对象，其状态（即对象中的数据）就不能被改变。
在Java中，不可变对象的内容一旦被创建后就不能被修改，任何修改都会导致创建一个新的对象。
Java中除了String外，Integer、Float、BigInteger等包装类也是不可变的。