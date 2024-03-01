package gitlet;

import java.io.IOException;
import java.io.StringReader;
import java.util.*;

import static gitlet.GitletConstants.*;
import static gitlet.Utils.*;
import static gitlet.IndexUtils.indexMap;
import static gitlet.IndexUtils.stagedFileContents;

/**
 * @description Represents a gitlet repository. Provide helper functions called by Main method.
 * For example: when enter (git add), Main method will call relevant helper method in this Repository class
 */
public class Repository {
    /** HEAD pointer, this pointer points to current branch name, not explicit commit id, for example HEAD == "master" */
    public static String HEAD;

    /**
     * @return boolean: checkout if this project is gitlet initialized
     * */
    public static boolean isInitialized() {
        return GITLET_DIR.exists();
    }

    // if .gitlet is initialized, we have to set HEAD to proper branch, e.g. master branch
    static {
        if (isInitialized()) {
            HEAD = new String(readContents(HEAD_FILE));
        }
    }

    /**
     * @description
     * 1. Init the repository and create the .gitlet folder
     * 2. create empty index file for git add command
     * 3. create commits/....(commit id/ SHA-1) to store a empty commit
     * 4. create branches/master to store the first commit id, means master --> first commit
     * 5. create HEAD file, and store master in this file, means HEAD --> master
     * */
    public static void init() {
        if (isInitialized()) {
            System.out.println("A Gitlet version-control system already exists in the current directory.");
            return;
        }
        if (!GITLET_DIR.mkdir()) {
            System.out.println("Fail to create .gitlet folder in this work directory.");
            return;
        }

        // according to the logical above, following code will only be executed once
        // create critical folders and files for .gitlet
        try {
            INDEX_FILE.createNewFile(); // at first, the index file will be empty
            HEAD_FILE.createNewFile();
            STAGED_FILE.createNewFile(); // stage file contents
        } catch (IOException e) {
            throw new RuntimeException("failed to create INDEX file and HEAD file");
        }
        COMMITS_DIR.mkdir();
        OBJECTS_DIR.mkdir();
        BRANCHES_DIR.mkdir();

        // store & submit first empty commit
        Commit initialCommit = CommitUtils.makeEmptyCommit("initial commit");
        String initialCommitID = CommitUtils.saveCommit(initialCommit);

        // generate master branch --> initial commit
        BranchUtils.saveCommitId(MASTER_BRANCH_NAME, initialCommitID);

        // HEAD --> MASTER
        setHEAD(MASTER_BRANCH_NAME);
    }

    /***
     * In gitlet, only one file may be added at a time.
     * but this function supports add multiple files at once
     * @param fileName the name of the file want to be added
     */
    public static void add(String fileName) {
        if (!join(CWD, fileName).exists()) {
            System.out.println("File does not exist.");
            return;
        }

        // maybe we should only update index; because after every commit, index and commit-map are same;
        if (indexMap.containsKey(fileName)) {
            String targetSHA1 = indexMap.get(fileName);
            // if the file is same as current index files, means no change, then directly return
            if (FileUtils.hasSameSHA1(fileName, targetSHA1)) return;
        }

        IndexUtils.stageFile(fileName);
        IndexUtils.saveIndex();
    }

    /***
     * It is not a failure for tracked files to be missing from the working directory or changed in the working directory
     * advised implementation: Saves [a snapshot of tracked files] in the current commit and [staging area] so they can be restored at a later time
     * note: this is the magic of sha-1: if commit 1 & 3 has the same test.txt, then commit 3 will just overwrite the test.txt object file(same sha1)
     * @param commitMessage every commit must contain a commit message
     */
    public static void commit(String commitMessage) {
        if (commitMessage.isEmpty()) {
            System.out.println("Please enter a commit message.");
            return;
        }
        String currentCommitId = getHeadCommitId();
        Commit currentCommit = CommitUtils.readCommit(currentCommitId);
        HashMap<String, String> fileVersionMap = currentCommit.getFileVersionMap();
        // bug: fileVersionMap may be null, but indexMap will never be null(after git init)
        if (indexMap.equals(fileVersionMap)) {
            // note: this implementation is different from the proj2 doc
            System.out.println("No changes added to the commit.");
        }
        Commit newCommit = CommitUtils.makeCommit(commitMessage, currentCommitId, indexMap);
        CommitUtils.createFileObjects(currentCommit, newCommit, stagedFileContents); // create the files (different from the last commit)
        stagedFileContents.clear();
        IndexUtils.saveIndex(); // clear and save
        String newCommitId = CommitUtils.saveCommit(newCommit);
        BranchUtils.saveCommitId(HEAD, newCommitId); // save current branch pointer --> new commit id
    }

    /***
     * the logic will only delete explicit file in work directory, not in .gitlet
     * @param fileName the file name in work directory
     */
    public static void rm(String fileName) {
        Commit commit = CommitUtils.readCommit(getHeadCommitId());
        boolean staged = IndexUtils.isStaged(fileName, commit);
        boolean trackedByHeadCommit = CommitUtils.isTrackedByCommit(commit, fileName);
        if (!staged && !trackedByHeadCommit) {
            System.out.println("No reason to remove the file.");
            return;
        }
        IndexUtils.unstageFile(fileName);
        IndexUtils.saveIndex(); // note: all changes must be saved
        if (trackedByHeadCommit) {
            // if it is tracked by current commit, you should delete the file in CWD.
            restrictedDelete(join(CWD, fileName));
        }
    }

    /***
     * trace commit chain from head->commit to initial commit
     */
    public static void log() {
        Commit currentCommit = CommitUtils.readCommit(getHeadCommitId());
        List<Commit> commits = CommitUtils.commitTraceBack(currentCommit);
        for (Commit commit : commits) {
            commit.printCommitInfo();
        }
    }

    /***
     * global-log: print all commits with random order
     */
    public static void globalLog() {
        List<String> commitIdList = plainFilenamesIn(COMMITS_DIR);
        if (commitIdList == null || commitIdList.isEmpty()) {
            return;
        }
        for (String commitId : commitIdList) {
            CommitUtils.readCommit(commitId).printCommitInfo();
        }
    }

    /**
     * Prints out the ids of all commits that have the given commit message, one per line
     */
    public static void find(String commitMessage) {
        List<String> commitIdList = plainFilenamesIn(COMMITS_DIR);
        if (commitIdList == null || commitIdList.isEmpty()) {
            return;
        }
        boolean printFlag = false;
        for (String commitId : commitIdList) {
            Commit commit = CommitUtils.readCommit(commitId);
            if (commitMessage.equals(commit.getMessage())) {
                System.out.println(CommitUtils.getCommitId(commit));
                printFlag = true;
            }
        }
        if (!printFlag) {
            System.out.println("Found no commit with that message.");
        }
    }

    /***
     * just an checkout interface, this command will be done by one of its different methods it calls
     * @param args rest args from command line.
     */
    public static void checkout(String...args) {
        Commit commit = null;
        if (args.length > 1) {
            String fileName;
            if (args.length == 2) {
                if (!args[0].equals("--")) {
                    System.out.println("Incorrect operands.");
                }
                fileName = args[1];
                commit = CommitUtils.readCommit(getHeadCommitId());
            } else {
                if (!args[1].equals("--")) {
                    System.out.println("Incorrect operands.");
                }
                fileName = args[2];
                commit = CommitUtils.readCommitByPrefix(args[0]);
                if (commit == null) {
                    System.out.println("No commit with that id exists.");
                    return;
                }
            }
            checkoutFile(commit, fileName);
        } else {
            commit = CommitUtils.readCommit(getHeadCommitId());
            checkoutBranch(commit, args[0]);
        }
    }

    /**
     * change to new branch's pointer commit, just like the new branch's commit just happen.
     * so indexMap(& .gitlet/index) is the same as the new branch commit fileVersionMap, stagedFiles(& .gitlet/staged-files) is cleared.
     * @param commit current commit object (before branch change)
     * @param branchName the name of the branch to be changed to
     */
    private static void checkoutBranch(Commit commit, String branchName) {
        if (!BranchUtils.branchExists(branchName)) { // branchExists() will assert branchName != null
            System.out.println("No such branch exists.");
            return;
        }
        if (branchName.equals(HEAD)) {
            System.out.println("No need to checkout the current branch.");
            return;
        }
        List<String> CWDFileNames = plainFilenamesIn(CWD);
        assert CWDFileNames != null;
        for (String fileName : CWDFileNames) {
            if (!CommitUtils.isTrackedByCommit(commit, fileName)) {
                System.out.println("There is an untracked file in the way; delete it, or add and commit it first.");
                return;
            }
        }
        // restore commit to CWD
        Commit newBranchCommit = CommitUtils.readCommit(BranchUtils.getCommitId(branchName));
        restoreCommit(newBranchCommit);

        // 3. set HEAD == new branch name
        setHEAD(branchName);
    }

    /**
     * restore this commit to CWD, and restore index region(clear stagedFileContents and restore indexMap)
     * just like the commit just happen.
     */
    private static void restoreCommit(Commit commit) {
        Commit currentCommit = CommitUtils.readCommit(getHeadCommitId());
        // pre-check
        for (String fileName : Objects.requireNonNull(plainFilenamesIn(CWD))) {
            if (FileUtils.isOverwritingOrDeletingCWDUntracked(fileName, currentCommit)) {
                System.out.println(MERGE_MODIFY_UNTRACKED_WARNING);
                return;
            }
        }

        // 1. restore files to CWD
        FileUtils.restoreCommitFiles(commit);

        // 2. restore indexMap
        // note: to keep consistency, checkout branch just like the new branch's commit() just happen
        // so it will restore indexMap & .gitlet/index, but stagedFiles and its file stay empty.
        indexMap = commit.getFileVersionMap();
        stagedFileContents.clear();
        IndexUtils.saveIndex();
    }

    /***
     * hints: The new version of the file is not staged. it means we should NOT change staged area
     */
    public static void checkoutFile(Commit commit, String fileName) {
        if (!CommitUtils.isTrackedByCommit(commit, fileName)) {
            System.out.println("File does not exist in that commit.");
            return;
        }
        String fileSHA1 = commit.getFileVersionMap().get(fileName);
        String fileContent = FileUtils.getFileContent(fileSHA1);
        FileUtils.writeCWDFile(fileName, fileContent);
    }

    /***
     * Creates a new branch with the given name, and points it at the current head commit.
     * @param branchName the new branch name you create.
     */
    public static void branch(String branchName) {
        if (BranchUtils.branchExists(branchName)) {
            System.out.println("A branch with that name already exists.");
            return;
        }
        BranchUtils.saveCommitId(branchName, getHeadCommitId());
    }

    /**
     * delete branch file of branchName
     */
    public static void removeBranch(String branchName) {
        if (!BranchUtils.branchExists(branchName)) {
            System.out.println("A branch with that name does not exist.");
            return;
        }
        if (HEAD.equals(branchName)) {
            System.out.println("Cannot remove the current branch.");
            return;
        }
        BranchUtils.removeBranch(branchName);
    }

    /**
     * print out some status message
     */
    public static void status() {
        // print branches
        List<String> allBranchNames = BranchUtils.getAllBranchNames();
        System.out.println("=== Branches ===");
        for (String branchName : allBranchNames) {
            System.out.println((HEAD.equals(branchName) ? "*" : "") + branchName);
        }
        System.out.println();

        // print staged files
        Commit commit = CommitUtils.readCommit(getHeadCommitId());
        List<String> stagedFileNames = IndexUtils.getStagedFiles(commit);
        System.out.println("=== Staged Files ===");
        stagedFileNames.forEach(System.out::println);
        System.out.println();

        // print removed files
        List<String> removedFileNames = IndexUtils.getRemovedFiles(commit);
        System.out.println("=== Removed Files ===");
        removedFileNames.forEach(System.out::println);
        System.out.println();

        System.out.println("=== Modifications Not Staged For Commit ===");
        List<StringBuffer> modifiedNotStagedForCommit = IndexUtils.modifiedNotStagedForCommit(commit);
        List<StringBuffer> deletedNotStagedForCommit = IndexUtils.deletedNotStagedForCommit(commit);
        modifiedNotStagedForCommit.forEach(s -> s.append(" (modified)"));
        deletedNotStagedForCommit.forEach(s -> s.append(" (deleted)"));
        modifiedNotStagedForCommit.addAll(deletedNotStagedForCommit);
        modifiedNotStagedForCommit.sort(StringBuffer::compareTo);
        modifiedNotStagedForCommit.forEach(System.out::println);
        System.out.println();

        // ("Untracked Files") is for files present in the working directory but neither staged for addition nor tracked.
        System.out.println("=== Untracked Files ===");
        List<String> untrackedFileNames = IndexUtils.getUntrackedFiles(commit);
        untrackedFileNames.forEach(System.out::println);
        System.out.println();
    }

    /**
     * The command is essentially checkout of an arbitrary commit that also changes the current branch head.
     */
    public static void reset(String commitIdPrefix) {
        Commit commit = CommitUtils.readCommitByPrefix(commitIdPrefix);
        if (commit == null) {
            System.out.println("No commit with that id exists.");
            return;
        }
        String commitId = CommitUtils.getCommitId(commit);
        restoreCommit(commit);
        BranchUtils.saveCommitId(HEAD, commitId);
    }

    /**
     * @note
     * If an untracked file in the current commit would be overwritten or deleted by the merge, print There is an
     * untracked file in the way; delete it, or add and commit it first. and exit; perform this check before doing
     * anything else.
     */
    public static void merge(String branchName) {
        // pre check fail cases
        if (!BranchUtils.branchExists(branchName)) {
            System.out.println("A branch with that name does not exist.");
            return;
        }
        if (HEAD.equals(branchName)) {
            System.out.println("Cannot merge a branch with itself.");
            return;
        }
        Commit currentCommit = CommitUtils.readCommit(getHeadCommitId());
        List<String> stagedFileNames = IndexUtils.getStagedFiles(currentCommit);
        List<String> removedFileNames = IndexUtils.getRemovedFiles(currentCommit);
        if (!stagedFileNames.isEmpty() || !removedFileNames.isEmpty()) {
            System.out.println("You have uncommitted changes.");
            return;
        }
        // get current-branch commit, target-branch commit & split point commit
        Commit branchCommit = CommitUtils.readCommit(BranchUtils.getCommitId(branchName));
        // Commit splitPoint = CommitUtils.getSplitCommit(HEAD, branchName);
        Commit splitPoint = CommitUtils.getSplitCommitWithGraph(HEAD, branchName);

        // the cases in which the current branch and target branch in the same line
        if (splitPoint == null || CommitUtils.isSameCommit(branchCommit, splitPoint)) {
            System.out.println("Given branch is an ancestor of the current branch.");
            return; // in this case, head & branch points to the same commit, no need to merge
        }
        if (CommitUtils.isSameCommit(currentCommit, splitPoint)) {
            checkout(branchName); // checkout branch
            // fast-forward master pointer
            BranchUtils.saveCommitId(HEAD, BranchUtils.getCommitId(branchName));
            System.out.println("Current branch fast-forwarded.");
            return;
        }

        // Complex situation: merge with no conflict or merge with conflict
        Set<String> splitPointFiles = splitPoint.getFileVersionMap().keySet();
        Set<String> currentCommitFiles = currentCommit.getFileVersionMap().keySet();
        Set<String> branchCommitFiles = branchCommit.getFileVersionMap().keySet();
        // union the upper three set to get all relevant files in three commits
        // bug: you have to allocate new memory, not reference
        Set<String> allRelevantFiles = new HashSet<>(splitPointFiles); // there is other usage with variable splitPointFiles
        allRelevantFiles.addAll(currentCommitFiles);
        allRelevantFiles.addAll(branchCommitFiles);

        boolean conflictFlag = false;

        for (String fileName : allRelevantFiles) {
            boolean splitCurrentConsistent = CommitUtils.isConsistent(fileName, splitPoint, currentCommit);
            boolean splitBranchConsistent = CommitUtils.isConsistent(fileName, splitPoint, branchCommit);
            boolean branchCurrentConsistent = CommitUtils.isConsistent(fileName, currentCommit, branchCommit);
            // merge no conflicts
            if ((splitBranchConsistent && !splitCurrentConsistent) || branchCurrentConsistent) {
                continue;
            }

            if (!splitBranchConsistent && splitCurrentConsistent) {
                if (!branchCommitFiles.contains(fileName)) {
                    // in this case, other two commit must contain the file
                    // remove the file from CWD & not tracked this file in merged commit
                    // which means drop indexMap's record with this fileName
                    if (FileUtils.isOverwritingOrDeletingCWDUntracked(fileName, currentCommit)) { // safety check is needed
                        System.out.println(MERGE_MODIFY_UNTRACKED_WARNING);
                        return;
                    } else {
                        rm(fileName);
                    }
                } else {
                    // in this case, we will checkout the file in branchCommit and add it to index
                    if (FileUtils.isOverwritingOrDeletingCWDUntracked(fileName, currentCommit)) { // safety check is needed
                        System.out.println(MERGE_MODIFY_UNTRACKED_WARNING);
                        return;
                    } else {
                        checkoutFile(branchCommit, fileName);
                        add(fileName);
                    }
                }
                continue;
            }

            // merge with conflicts, if logic can be simplified
            if (!splitBranchConsistent && !splitCurrentConsistent && !branchCurrentConsistent) {
                conflictFlag = true;
                StringBuilder conflictedContents = new StringBuilder("<<<<<<< HEAD\n");
                String currentCommitContent =  currentCommitFiles.contains(fileName) ?
                                               FileUtils.getFileContent(fileName, currentCommit) : "";
                String branchCommitContent = branchCommitFiles.contains(fileName) ?
                                             FileUtils.getFileContent(fileName, branchCommit) : "";
                conflictedContents.append(currentCommitContent);
                conflictedContents.append("=======\n");
                conflictedContents.append(branchCommitContent);
                conflictedContents.append(">>>>>>>\n");
                if (FileUtils.isOverwritingOrDeletingCWDUntracked(fileName, currentCommit)) { // safety check is needed
                    System.out.println(MERGE_MODIFY_UNTRACKED_WARNING);
                    return;
                } else {
                    FileUtils.writeCWDFile(fileName, String.valueOf(conflictedContents));
                    add(fileName);
                }
            }
        }

        // 1. make commit 2. set this new commit secondParentId
        commit("Merged " + branchName + " into " + HEAD + ".");
        Commit mergeCommit = CommitUtils.readCommit(getHeadCommitId());
        mergeCommit.setSecondParentId(BranchUtils.getCommitId(branchName));
        // bug: you have to save the merge commit. all changes must be saved
        CommitUtils.saveCommit(mergeCommit);

        // 3. other things to do: you have to make the current branch --> merged commit
        BranchUtils.saveCommitId(HEAD, CommitUtils.getCommitId(mergeCommit));

        // if conflicted, you should out put some message
        if (conflictFlag) {
            System.out.println("Encountered a merge conflict.");
        }
    }


    /**
     * It set HEAD --> branch_name (other function maybe about set head on commit,
     * but this project will ignore this situation)
     * At the same time, it saves the HEAD file
     * @param branchName the param must exist, otherwise it will throw AssertionError
     * */
    public static void setHEAD(String branchName) {
        assert BranchUtils.branchExists(branchName);
        HEAD = branchName;
        writeContents(HEAD_FILE, branchName);
    }

    /***
     * head --> branch name --> commit id
     */
    public static String getHeadCommitId() {
        return BranchUtils.getCommitId(HEAD);
    }

}
