package gitlet;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;

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
        boolean staged = indexMap.containsKey(fileName);
        boolean trackedByHeadCommit = CommitUtils.isTrackedByCommit(getHeadCommitId(), fileName);
        if (!staged && !trackedByHeadCommit) {
            System.out.println("No reason to remove the file.");
        }
        IndexUtils.unstageFile(fileName);
        IndexUtils.saveIndex(); // note: all changes must be saved
        if (trackedByHeadCommit) {
            restrictedDelete(fileName);
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
        if (commitIdList == null) {
            return;
        }
        for (String commitId : commitIdList) {
            CommitUtils.readCommit(commitId).printCommitInfo();
        }
    }

    /***
     * just an checkout interface, this command will be done by one of its different methods it calls
     * @param args rest args from command line.
     */
    public static void checkout(String...args) {
        if (args.length > 1) {
            String fileName;
            Commit commit;
            if (args.length == 2) {
                fileName = args[1];
                commit = CommitUtils.readCommit(getHeadCommitId());
            } else {
                fileName = args[2];
                commit = CommitUtils.readCommitByPrefix(args[0]);
                if (commit == null) {
                    System.out.println("No commit with that id exists.");
                }
            }
            checkoutFile(commit, fileName);
        } else {
            System.out.println("branch function developing...");
        }
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


    /**
     * It set HEAD --> branch_name (other function maybe about set head on commit,
     * but this project will ignore this situation)
     * At the same time, it changes the HEAD file
     * */
    public static void setHEAD(String branchName) {
        HEAD = MASTER_BRANCH_NAME;
        writeContents(HEAD_FILE, branchName);
    }

    /***
     * head --> branch name --> commit id
     */
    public static String getHeadCommitId() {
        return BranchUtils.getCommitId(HEAD);
    }

}
