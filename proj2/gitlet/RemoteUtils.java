package gitlet;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static gitlet.GitletConstants.*;
import static gitlet.Utils.*;

/**
 * @Author 3590
 * @Date 2024/3/2 0:18
 * @Description
 * For remotes (like skeleton which we’ve been using all semester),
 * we’ll simply use other Gitlet repositories.
 * Pushing simply means copying all commits and blobs that the remote
 * repository does not yet have to the remote repository, and resetting a branch reference.
 * Pulling is the same, but in the other direction.
 */
public class RemoteUtils {
    public static TreeMap<String, String> remoteLocationMap = new TreeMap<>();

    static {
        if (remoteRefsInitialized()) {
            remoteLocationMap = readObject(REMOTE_FILE, TreeMap.class);
        }
    }

    public static void saveRemoteLocationMap() {
        writeObject(REMOTE_FILE, remoteLocationMap);
    }

    public static boolean remoteRefsInitialized() {
        return REMOTE_FILE.exists();
    }

    /**
     * ../remote/.gitlet, yes, you will get the remote .gitlet path
     */
    public static String getRemotePath(String remoteName) {
        return remoteLocationMap.get(remoteName);
    }

    public static File getRemoteGitletFolder(String remoteName) {
        return Utils.join(getRemotePath(remoteName));
    }

    public static boolean isRemoteAdded(String remoteName) {
        return remoteLocationMap.containsKey(remoteName);
    }


    public static File remoteCommitsFolder(String remoteName) {
        return join(getRemoteGitletFolder(remoteName), "commits");
    }

    public static File remoteBranchesFolder(String remoteName) {
        return join(getRemoteGitletFolder(remoteName), "branches");
    }

    public static File remoteObjectsFolder(String remoteName) {
        return join(getRemoteGitletFolder(remoteName), "objects");
    }

    public static void copyCommitFileToRemote(String commitId, String remoteName) {
        if (!isRemoteAdded(remoteName)) {
            return;
        }
        File remoteCommitsFolder = remoteCommitsFolder(remoteName);
        File remoteCommitFile = join(remoteCommitsFolder, commitId);
        writeObject(remoteCommitFile, CommitUtils.readCommit(commitId));
    }

    public static void copyCommitFileFromRemote(String commitId, String remoteName) {
        File remoteCommitsFolder = remoteCommitsFolder(remoteName);
        File remoteCommitFile = join(remoteCommitsFolder, commitId);
        Commit commit = readObject(remoteCommitFile, Commit.class);
        CommitUtils.saveCommit(commit);
    }

    public static void copyBranchFileToRemote(String branchName, String remoteName) {
        if (!isRemoteAdded(remoteName)) {
            return;
        }
        if (!BranchUtils.branchExists(branchName)) {
            return;
        }
        String branchContent = readContentsAsString(join(BRANCHES_DIR, branchName));
        File remoteBranchesFolder = remoteBranchesFolder(remoteName);
        File remoteBrancheFile = join(remoteBranchesFolder, branchName);
        writeContents(remoteBrancheFile, branchContent);
    }

    public static void copyObjectsFileToRemote(String fileSHA1, String remoteName) {
        String fileContent = FileUtils.getFileContent(fileSHA1);
        File remoteObjectsFolder = remoteObjectsFolder(remoteName);
        File remoteObjectFile = join(remoteObjectsFolder, fileSHA1);
        writeContents(remoteObjectFile, fileContent);
    }

    public static void copyObjectsFileFromRemote(String fileSHA1, String remoteName) {
        File remoteObjectsFolder = remoteObjectsFolder(remoteName);
        File remoteObjectFile = join(remoteObjectsFolder, fileSHA1);
        String fileContent = readContentsAsString(remoteObjectFile);
        FileUtils.writeGitletObjectsFile(fileContent);
    }

    public static String readRemoteHEAD(String remoteName) {
        return readContentsAsString(join(getRemotePath(remoteName), "HEAD"));
    }

    public static void writeRemoteHEAD(String remoteName, String content) {
        writeContents(join(getRemotePath(remoteName), "HEAD"), content);
    }

    public static boolean remoteBranchExists(String branchName, String remoteName) {
        File remoteBranchesFolder = remoteBranchesFolder(remoteName);
        List<String> stringList = plainFilenamesIn(remoteBranchesFolder);
        if (stringList == null) {
            return false;
        }
        return stringList.contains(branchName);
    }

    /***
     * @return if commitId is null, then return null. else, return the commit object in remote repo
     * */
    public static Commit readRemoteCommit(String commitId, String remoteName) {
        if (commitId == null) {
            return null;
        }
        return readObject(join(remoteCommitsFolder(remoteName), commitId), Commit.class);
    }

    /**
     * trace back commits in remote repo. the result will include current commit.
     */
    public static List<Commit> remoteCommitTraceback(String commitId, String remoteName) {
        Commit commit = readRemoteCommit(commitId, remoteName);
        Commit ptr = commit;
        List<Commit> result = new LinkedList<>();
        while (ptr != null) {
            result.add(ptr);
            ptr = readRemoteCommit(ptr.getParentId(), remoteName);
        }
        return result;
    }

    /**
     * trace back commits in remote repo. the result will include current commit.
     */
    public static List<String> remoteCommitIdTraceback(String commitId, String remoteName) {
        Commit commit = readRemoteCommit(commitId, remoteName);
        Commit ptr = commit;
        List<String> result = new LinkedList<>();
        while (ptr != null) {
            result.add(CommitUtils.getCommitId(ptr));
            ptr = readRemoteCommit(ptr.getParentId(), remoteName);
        }
        return result;
    }

    /**
     * if branch not exists in remote branch, then return null
     * @return the branch's commit id
     */
    public static String readRemoteBranch(String branchName, String remoteName) {
        if (!remoteBranchExists(branchName, remoteName)) {
            return null;
        }
        File remoteBranchesFolder = remoteBranchesFolder(remoteName);
        return readContentsAsString(join(remoteBranchesFolder, branchName));
    }

    /**
     * it does two things:
     * 1. add remoteName --> remotePath to remoteLocationMap and save it
     * 2. create folder for this remote
     * remotePath maybe ../testing/otherdir/.gitlet
     */
    public static void addRemote(String remoteName, String remotePath) {
        if (!REMOTE_FILE.exists()) {
            try {
                REMOTE_FILE.createNewFile();
            } catch (IOException e) {
                throw new RuntimeException("failed to create remote file");
            }
        }
        if (isRemoteAdded(remoteName)) {
            System.out.println("A remote with that name already exists.");
            return;
        }
        String[] split = remotePath.split("/");
        StringBuilder convertedPath = new StringBuilder();
        for (String elem : split) {
            convertedPath.append(elem);
            convertedPath.append(File.separator); // the correct separator, windows for \, linux for /
        }
        convertedPath.delete(convertedPath.length() - 1, convertedPath.length());
        remoteLocationMap.put(remoteName, String.valueOf(convertedPath));
        saveRemoteLocationMap();
    }

    public static void removeRemote(String remoteName) {
        if (!remoteRefsInitialized()) {
            return;
        }
        if (!isRemoteAdded(remoteName)) {
            System.out.println("A remote with that name does not exist.");
            return;
        }
        remoteLocationMap.remove(remoteName);
        saveRemoteLocationMap();
    }

    public static void push(String remoteName, String remoteBranchName) {
        if (!getRemoteGitletFolder(remoteName).exists()) {
            System.out.println("Remote directory not found.");
            return;
        }
        String remoteHEAD = readRemoteHEAD(remoteName);
        String remoteHEADCommitId = readRemoteBranch(remoteHEAD, remoteName);
        Commit currentCommit = CommitUtils.readCommit(Repository.getHeadCommitId());
        if (Repository.getHeadCommitId().equals(remoteHEADCommitId)) {
            return;
        }
        // just think this problem as a linked list, not complicated Graph
        // the order is the newest commit(front) --> older commit --> initial commit
        List<String> historyCommitId = CommitUtils.commitIdTraceBack(currentCommit);
        if (!historyCommitId.contains(remoteHEADCommitId)) {
            System.out.println("Please pull down remote changes before pushing.");
            return;
        }
        int remoteIdx = historyCommitId.indexOf(remoteHEADCommitId);
        List<String> commitIdAppending = historyCommitId.subList(0, remoteIdx);
        Collections.reverse(commitIdAppending); // from remote id's next --> newest [not contains the remote HEAD commit]
        // append future commit to remote branch
        for (String commitId : commitIdAppending) {
            // 1. copy the commit file
            copyCommitFileToRemote(commitId, remoteName);
            // 2. copy the commit objects
            Commit commit = CommitUtils.readCommit(commitId);
            HashMap<String, String> fileVersionMap = commit.getFileVersionMap();
            for (String fileName : fileVersionMap.keySet()) {
                copyObjectsFileToRemote(fileVersionMap.get(fileName), remoteName);
            }
        }
        // add this branch (or overwriting this branch)
        copyBranchFileToRemote(remoteBranchName, remoteName);
        // set HEAD points to this branch, note: HEAD always points to BRANCH NAME!
        writeRemoteHEAD(remoteName, remoteBranchName);
    }

    public static void fetch(String remoteName, String remoteBranchName) {
        if (!getRemoteGitletFolder(remoteName).exists()) {
            System.out.println("Remote directory not found.");
            return;
        }
        if (!remoteBranchExists(remoteBranchName, remoteName)) {
            System.out.println("That remote does not have that branch.");
            return;
        }
        // 1. copies all commits and blobs from the given branch in the remote repository
        String remoteCommitId = readRemoteBranch(remoteBranchName, remoteName);
        List<String> allTracedCommitIds = remoteCommitIdTraceback(remoteCommitId, remoteName);
        // copy these commit files to local
        for (String commitId : allTracedCommitIds) {
            copyCommitFileFromRemote(commitId, remoteName);
            // copy blobs to local
            Commit commit = readRemoteCommit(commitId, remoteName);
            HashMap<String, String> fileVersionMap = commit.getFileVersionMap();
            for (String fileName : fileVersionMap.keySet()) {
                copyObjectsFileFromRemote(fileVersionMap.get(fileName), remoteName);
            }
        }
        // create a new branch named [remote name]/[remote branch name] in local repo & points to remote head commit
        // note: because windows not allowed '/' or '\' in file name, so we will create a folder, and save the commit.
        BranchUtils.saveCommitId(remoteName + "/" + remoteBranchName, remoteCommitId);
    }

    public static void pull(String remoteName, String remoteBranchName) {
        fetch(remoteName, remoteBranchName);
        String mergedBranchName = remoteName + "/" + remoteBranchName;
        Repository.merge(mergedBranchName);
    }

}
