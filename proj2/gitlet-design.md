# GitLet 设计文档
- 实现一个简化版本的 git。由于代码编译中有中文注释会报错，故代码中的所有说明皆以英文来写。
- 网上博客对于 git 设计的理解未必正确，如果希望更好地理解 git，最好的方式还是查阅 git 官方文档。
- 这是UC Berkeley CS61B的一个课程项目，项目说明文档：https://sp21.datastructur.es/materials/proj/proj2/proj2#global-log
- 此外，为了设计存储方式和存储的文件结构，还需要参考 git 官方文档：https://www.progit.cn/#_plumbing_porcelain

相比于git，主要的简化如下：
- ①只处理工作区包含平面文件的情况，不考虑工作区添加文件夹，并在文件夹添加文件的情况。
- ②一个提交最多有两个parent提交，只有在合并分支的时候，产生的合并提交会有两个parent commit。
- ③处理合并冲突的方式大幅简化，只是将两个文件的内容拼接在一起。而git本身处理合并冲突是十分智能的，可以把合并冲突精确到文件某行。
- ④合并冲突的情况下也会进行一次提交，而git在合并冲突时不会提交，必须解决所有冲突后才能提交。
- ⑤远程命令的实现进行了较大幅度简化，只考虑一般的简单情况。

# 主要工具类
## Main
- 通过命令行传入 Main(String...args) 中的args。在调用具体命令对应的方法之前，该类会对参数个数是否符合要求和仓库是否初始化进行检查。
- 分支很多时，为了减少编程错误，最好使用反射，因为 switch 分支忘记写 break 的概率极高，项目运行时总会带来意外之喜。但是本课程的教授似乎对反射嗤之以鼻。
- 因此，这里使用Java8新特性：方法引用，以实现每个命令的检查、调用与传参，这样做可以减少调用和传参时发生的错误。 
- 每个命令具体实现在 Repository.java 中。
- 神奇的是，Runnable 在 Java8 中可以当作 Function，它不接受参数，返回是 void。此时，它并不应用于多线程，而是函数接口。
- 执行一个命令相当于运行了一次main函数，所以内存中的所有变量在命令结束后都会被销毁，如果需要保存信息，需要持久化到磁盘，写入文件。

## Repository
**初始化**
如果仓库已经初始化，即调用了init()，则将 .gitlet/HEAD 读取到 HEAD 变量中，HEAD 表示指向的分支名称。

**变量**
- HEAD: 维护了 HEAD 指针，与 .gitlet/HEAD 关联。在程序启动时，如果已经初始化，就从 .gitlet 中读取 HEAD 文件，
  HEAD 文件存放的是分支名。比如 HEAD = master。

**功能**
- 实现了各类命令的调用方法。比如，命令行中输入 add file.txt，那么会调用其中的 add() 函数。

## Commit
普通的 JavaBean，记录提交的信息。

**变量**
- message(String): 每次提交的信息
- commitTime(Date): 提交时间(注意所有仓库init()初始提交的时间都是时间戳开始时间，即1970年那个神奇的时间)
- parentId(String): 存储它上一个提交的id(id是对Commit对象序列化计算sha1得到的)
- secondParentId(String): 用于合并分支，合并后的分支会有两个parent commit，记录另一个parent commit 的id值
- fileVersionMap(Map<String, String>): commit的文件版本列表。存储键值对： 文件名 --> 文件版本(文件版本通过计算sha1得到)，
如果一个文件名包含在 fileVersionMap，就说**该文件被commit跟踪**。

**方法**
- printCommitInfo() 为了 log 命令准备的，打印该 commit 的相关信息。

## FileUtils
文件工具类，用于更方便地复用文件读入和读出代码。

**方法**
- restoreCommitFiles(Commit commit): 将某次提交commit的所有文件版本恢复到工作区，这涉及重写、添加、删除文件。使得工作区文件目录看起来
就像是刚刚执行完该 commit 提交一样。注意：运行该方法之前必须在工作区对当前提交未跟踪的文件进行检查，如果存在未跟踪文件，则不能执行该方法。
- isOverwritingOrDeletingCWDUntracked(String fileName, Commit currentCommit)：fileName是将要在工作区被重写或删除的文件名，在这个
文件真正被删除或重写之前，检查它是否未被当前提交跟踪。

## BranchUtils
提供一些与分支相关的接口，它们的功能是包括：
- 可以通过分支名获取或写入该分支名目前指向的提交id。
- 可以查看目前仓库有哪些分支。
- 可以判断某个分支是否存在。

**特殊说明**
- 一般情况下，分支储存在 .gitlet/branches 目录中，文件名是分支名，文件内容是该分支指向的提交id，例如：.gitlet/master
- 对于远程分支，比如 origin/master 这样的分支，由于 windows 系统命名规则，不允许文件名包含斜杠，所以会对远程仓库名称创建文件夹，
存储方式举例如下： .gitlet/origin/master。也实现了相关接口，使得可以通过分支名 "origin/master" 操作该分支文件。

## IndexUtils
用于处理暂存区保存到磁盘，以及写入暂存区的操作。维护两个变量：文件版本列表，暂存文件内容。

**初始化**
- 每次运行命令时，如果仓库已经初始化，则将.gitlet文件夹中对应的文件读入 indexMap 和 stagedFileContents 这两个变量里。

**变量**
- indexMap: 暂存区的文件版本列表。静态变量，存放 文件名称-->版本(sha1) 的映射关系，与 .gitlet/index 文件直接关联。它代表下一次提交的 fileVersionMap。
- stagedFileContents: 暂存文件内容。静态变量，存放 版本(sha1)-->文件具体内容，与 .gitlet/staged-files 文件直接关联，每次提交时会生成 objects 文件夹的文件对象，并清空 stagedFileContents 对应的文件及其本身。

**方法**
- stageFile(String fileName): 将工作区的一个文件计算sha1，分别存储到 indexMap 和 stagedFileContents 里，但是不会写入文件。
- unstageFile(String fileName): 将工作区的一个文件移出 indexMap 和 stagedFileContents，也不会对文件进行任何操作。该设计存在些许问题：
如果一个人 stageFile 了两次，每次都是不同版本，而 indexMap 中只能保留一个记录(只因它们的文件名相同)；当你 unstageFile 的时候，会清除 indexMap
中该文件名为 key 的记录 
- getStagedFiles(Commit commit): 获得所有暂存文件的文件名。commit是提交对象(比如它可以是当前提交，即最新一次提交)。
所谓暂存文件，是指 commit 的 fileVersionMap 中没有且 indexMap 中有的，或者 二者皆有但版本不一致的 所有文件。
- getRemovedFiles(Commit commit): 获得下次提交时要被删除的文件名。假设commit是当前提交对象，
则该函数会获取被当前commit跟踪但是不存在于 indexMap 的所有文件。
- isStaged(String fileName, Commit commit): 判断某个文件是否被暂存，暂存的定义和 getStagedFiles 一致。
- isRemoval(String fileName, Commit commit): 判断某个文件是否在下次提交时将被移除，判断规则和 getRemovedFiles 一致。
- getUntrackedFiles(Commit commit): 获得工作区未被当前commit跟踪的文件名。所谓未被跟踪，指的是在工作区中存在，而未被commit跟踪的文件。
- modifiedNotStagedForCommit(Commit commit): 返回文件名列表。逻辑复杂，具体查看该函数的注释即可。
- deletedNotStagedForCommit(Commit commit): ①添加暂存区但是在当前工作目录中不存在的文件；②没有被标记 Removal(将被移除)，但是被当前
提交跟踪并且在当前工作目录中不存在的文件。

**使用**
- 对于 indexMap 和 stagedFileContents 两个内存变量的修改，需要通过 saveIndex() 方法持久到内存中。

## CommitUtils
提供与 commit 相关的各类操作，具体而言：
- 生成 commit 对象
- commit 对象与其 id 互相获取
- 判断某文件是否被某个 commit 跟踪 
- 读取和写入 commit 到 .gitlet/commits。在 .gitlet/commits 目录中，存储了每个序列化的 commit 对象， commit对象序列化后的 sha1 是文件名，文件内容就是序列化后的 commit 对象，
可以通过读取该文件反序列化生成内存中的 commit 对象。
- 从某个 commit 追溯其祖先提交(图追溯，每个commit既考虑parentId，又考虑secondParentId)。这种做法虽然可以追溯到全部祖先，但是祖先是无序的。
- 从某个 commit 追溯其祖先提交(链式追溯，每个commit仅考虑parentId，不考虑secondParentId)，该功能的意义是，追溯的祖先提交是有序的。
- 在合并分支时，获取两个commit的最小公共祖先提交。
- 判断两个提交对应的同一个文件是否处于一致性状态。一致性：它指的是对于一个文件，如果A提交和B提交都没有跟踪该文件，或者两个提交都跟踪了该文件
  且内容完全一致，则该文件对于A和B两个提交而言是一致的。

**方法**
- getSplitCommitWithGraph(String branchName1, String branchName2): 寻找两个分支末端提交(对应两个commit对象)的最小公共祖先提交对象。该
方法的思路是：将所有提交对象看作有向图结构，分别从两个 commit 对象出发，进行DFS遍历(图追溯)，访问到的结点就是其祖先(认为 commit 也是其自身的祖先)；
两个集合或者列表取公共部分，得到所有公共祖先；所有公共祖先提交结点又构成了一个图，计算该图所有结点的入度，入度是0的结点就是最小公共结点。计算入度可以
通过获得每个结点出度的指向进行统计。

## RemoteUtils
- 实现远程仓库与本地仓库数据的相互传输，比如传递分支信息、blob对象、commit文件等，还能够在本地修改远程仓库的分支指向，HEAD指向。
- 由于远程仓库模块相对独立，所以将远程命令也实现在该工具类中。

**初始化**
- 如果 .gitlet/remote 文件存在，就将文件读取到 remoteLocationMap 变量中。

**变量**
- remoteLocationMap: 远程仓库地址列表。静态变量，记录了键值对：远程仓库名称 --> 远程仓库地址，例如 origin --> ../org/d1/.gitlet。对应文件 .gitlet/remote。

# 本地命令实现思路
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
进行一次提交，该提交跟踪的文件及其版本与 indexMap 一致。提示：对于 untracked file，git 在提交时会忽略它们，并且允许提交。
1. 如果 indexMap 和当前提交(进行本次提交之前的提交)的 fileVersionMap 是一致的，则不提交，因为没有改变。
2. 生成 commit 对象，叫做 newCommit；当前 commit 记录为 oldCommit
3. 随后将 oldCommit 和 newCommit 的 fileVersionMap 进行对比，将一些文件从 stagedFileContents 持久化到 .gitlet/objects 中。这种做法的缺陷在于，
由于仅和当前 commit 进行对比， 如果 commit1 commit3 包含相同版本的 hello.txt(v1)，commit2 包含不同版本的 hello.txt(v2)，在提交 commit3 时，会导致 v1 
版本的文件被重复写入一次。当然，这不会带来错误。
4. 清空 stagedFileContents 对应的文件 staged-files。显然， stagedFileContents 作为内存变量，本次命令结束后会自动释放内存。
5. 将当前 branch，比如 master 的指针指向新的 commit id
6. 在该提交结束后，新生成提交的文件版本列表和暂存区的文件版本列表一致，暂存文件内容被清空。

## rm
- 直观上，该命令移除暂存区的对应文件。它的思想是这样的：对于被当前提交跟踪的文件，用户应该先在 CWD 删除文件，再调用 rm() 在暂存区标记为删除(add 的反向操作)。
  因为 rm() 会记录下一次提交时，哪些文件会被添加，哪些文件会被删除。所以下面的执行逻辑中，如果用户没有在 CWD 删除文件，就帮他删除位于 CWD 的文件。
- 对于当前提交未跟踪的文件，属于用户新添加的文件，只进行暂存区级别的操作(提供 add 的反向操作)，不对 CWD 进行操作，它的状态只有两种 === Untracked Files === / === Staged Files ===。
- 核心：只有 tracked files 才会在 status() 中被标记为 === Removed files ===，表示将要在下一次提交中移除的文件，
  恰好是 添加 CWD 文件 --> add() --> commit() 的反向操作：删除 CWD 文件 --> rm() --> 。

核心功能依然是移除暂存区中的文件(或者将某文件在暂存区标记为移除)，移除规则如下：
1. 如果一个文件没有被暂存区和commit跟踪(在 commit 的 fileVersionMap 中)，报错，没有移除文件的必要。
2. 如果一个文件被暂存区跟踪，就将它移除暂存区，注意，这也需要保存，调用saveIndex().
3. 如果文件被 commit 跟踪，就在工作区中删除该文件。 清空 stagedFileContents 对应的文件 staged-files

## checkout (file)
- checkout -- fileName: 恢复 head commit 储存的文件到工作区
- checkout [commit id] -- [file name]: 恢复某次 commit 对应的 file 到工作区
- 提示，这两个命令不会修改暂存区！
- 实现思路： 寻找到对应的 commit，然后通过它的 文件版本列表 获得 file 的版本，然后将该版本的文件拷贝到当前目录。

## checkout (branch)
将某个分支所指向的提交恢复到CWD，就好像该提交刚发生一样。它的实现如下：
1. 恢复该提交所有文件到工作区，使得工作区看起来和该提交刚发生时的目录结构一致。
2. 由于每次commit()会清空暂存文件内容，且保证暂存区文件版本列表与新生成的提交一致；
所以这里也需要清空暂存文件内容，且将该commit的文件版本列表拷贝到暂存区文件版本列表。
3. 修改HEAD指向新的branch，checkout branch 本质是修改 HEAD 的指向。

## find
它可以打印出 符合某个 commit message 的 commit 的所有 id。实现起来很容易，
直接去 .gitlet/commits 中逐个读取文件到内存然后分析其 commit message 即可。

## log
从当前提交链式向前追溯所有提交并依次打印即可。

## global-log
从 .gitlet/commits 中依次读取所有 commit 对象并打印相关信息即可。

## branch
创建分支，它需要做的是：在.gitlet/branches中创建一个新文件，文件名是 分支名，文件内容是head指针当前指向分支的对应的 commitId(HEAD commit id)。

## status
调用 IndexUtils 中提供的获取相关状态文件名的方法，然后依次打印这些文件名。

## rm-branch
将 .gitlet/branches 中的对应分支文件删除。其它文件或目录不做任何改变。

## reset
参数是提交的id
- checkout 某次提交，将该提交的所有文件恢复到工作目录。
- 移动分支的指针到该提交。

## merge(项目难点)
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

# 远程命令实现思路
这里通过两个 gitlet 仓库是分别作为本地仓库和远程仓库。在进行测试时，两个仓库都可以进行初始化、提交等所有本地命令。实现远程命令，主要就是完成本地
仓库和远程仓库之间的数据传送(修改远程仓库某个分支指针的指向也是数据传送的一种)。难点在于①如何设计函数，使得在本地操作远程仓库变得十分容易；②如何
从远程仓库读取相关信息，比如追溯历史记录等。

注意：下面的实现思路是基于更复杂的图结构的。而代码中实现了没有分支合并的简单情况(链式追溯)。而将代码的链式追溯改为图结构并不复杂，并且在 CommitUtils
中已经实现了这个功能。

## remote add
- 添加 远程仓库名 --> 远程仓库地址 到 .gitlet/remote 文件，即 远程仓库地址列表。
- 细节：需要将路径的斜杠方向变得和本系统一致。windows 是右斜杠，linux 是左斜杠。

## remote remove
- 在远程仓库地址列表中移除该远程仓库名对应的键值对。

## push [remote-name] [remote-branch]
提示：远程仓库的提交是本地仓库提交的历史版本，根据sha1的神奇特性，说明生成远程仓库提交的路径也是一样的。
- 读取远程仓库中 HEAD 对应的提交，通过图算法追溯本地该分支的所有提交，保证远程仓库的提交是本地仓库提交的历史版本，如果不是，该命令不允许执行。
- 如果远程仓库不存输入的分支名称，则在远程仓库创建这个分支，指向HEAD对应的提交。
- 在上述检查通过后，将该分支所有本地提交拷贝到远程仓库对应位置，这涉及到：拷贝这些提交对象的文件本身、拷贝这些提交对应文件版本列表中的所有blob对象。
  这样做是合理的，对于本地和远程分支共有的提交，可以覆盖写入，内容完全一致；对于远程没有的提交，本地将它拷贝过去，于是实现了分支同步。
- 将远程仓库分支的指针指向最新提交。

## fetch [remote-name] [remote-branch]
- 从远程仓库获得某个分支：做法很暴力：图追溯该分支的所有提交，直接将这些提交对象文件和blob文件拷贝到本地仓库对应位置。
- 然后在本地创建新分支，比如 origin/master，指向从远程仓库拷贝过来的最新提交，或者从远程仓库拷贝该分支的文件内容到本地。

## pull [remote-name] [remote-branch]
- fetch + merge。注意，由于该工程每个项目都有一个初始提交，其内容完全一致，所以所有分支至少有一个初始提交作为公共祖先。

# bug 记录区
- Main中的问题：switch 分支不加 break 会带来各种惊奇的错误
- 必要的函数传参中一定要断言或者判断是否是空指针，这也是为什么我们在比较字符串相等的时候常用 "hello".equals(pattern)，因为pattern很可能是空值
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