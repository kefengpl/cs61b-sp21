package gitlet;

import java.io.File;
import java.util.*;

import static gitlet.GitletConstants.*;
import static gitlet.Utils.*;


/**
 * @Author 3590
 * @Date 2024/2/20 21:06
 * @Description class for manipulate Commit, which is a JavaBean
 */
public class CommitUtils {
    /**
     * create an emptyCommit with no files -- version map
     * with no parent SHA-1 and no parent object(type also Commit) itself
     * @note you must set an empty HashMap to avoid null pointer
     * @param message commit message
     * @return the commit java bean
     * */
    public static Commit makeEmptyCommit(String message) {
        Commit commit = new Commit();
        commit.setMessage(message);
        commit.setCommitTime(new Date(0));
        commit.setParentId(null);
        commit.setSecondParentId(null);
        return commit;
    }

    /***
     * create a normal commit bean.
     * @note you must set an empty HashMap to avoid null pointer
     * @param message commit message, we will not check if this is empty
     * @param parentCommitId obvious parameter
     * @param fileVersionMap always from current index map, if == null, will be replaced by an empty hash map
     * @return commit bean
     */
    public static Commit makeCommit(String message,
                                    String parentCommitId, HashMap<String, String> fileVersionMap) {
        Commit commit = new Commit();
        commit.setMessage(message);
        commit.setParentId(parentCommitId);
        commit.setFileVersionMap(fileVersionMap == null ? new HashMap<>() : fileVersionMap);
        commit.setCommitTime(new Date());
        commit.setSecondParentId(null);
        return commit;
    }

    /***
     * save the commit bean to .gitlet/commits, with file name [SHA1], contents is the
     * serializable bean string.
     * @param commit the commit bean
     * @return commit id (SHA-1)
     */
    public static String saveCommit(Commit commit) {
        // note: we maybe use serialized string(byte[]) to calculate SHA-1 (not file)
        // because serialized object is string, which will be directly written to file.
        String CommitId = getCommitId(commit); // byte[] will be regarded as an Object
        File commitFile = join(COMMITS_DIR, CommitId);
        writeObject(commitFile, commit); // store our first commit
        return CommitId;
    }

    public static String getCommitId(Commit commit) {
        return sha1(serialize(commit));
    }

    /***
     * restore the commit java bean from its CommitId
     * @param commitId sha-1 of the commit
     */
    public static Commit readCommit(String commitId) {
        if (commitId == null) {
            return null;
        }
        return readObject(join(COMMITS_DIR, commitId), Commit.class);
    }

    /***
     * find correct commit bean with prefix of SHA-1
     * @param prefix prefix sha-1 of the commit
     * @warning this function as bugs, for example: prefix collisions
     * @return if read failed, if no exception, it will return null
     */
    public static Commit readCommitByPrefix(String prefix) {
        List<String> commitIdList = plainFilenamesIn(COMMITS_DIR);
        if (commitIdList == null) {
            return null;
        }
        int queryCount = 0;
        String resultCommitId = null;
        for (String commitId : commitIdList) {
            if (commitId.startsWith(prefix)) {
                queryCount++;
                resultCommitId = commitId;
            }
        }
        if (queryCount > 1) {
            throw new RuntimeException("this prefix is ambiguous, you must use longer prefix");
        }
        return readCommit(resultCommitId);
    }

    /***
     * compare the old commit map and new map, and create new objects in new map
     * note: directly save file from work directory is not safe, for user may change the content of the file in work directory
     * instead, we should save the file in memory, and save them to disk, to keep (sha1 <-- right content)
     */
    public static void createFileObjects(Commit oldCommit, Commit newCommit, HashMap<String, String> stagedFiles) {
        HashMap<String, String> oldFileVersion = oldCommit.getFileVersionMap();
        HashMap<String, String> newFileVersion = newCommit.getFileVersionMap();
        for (String fileName : newFileVersion.keySet()) {
            if (oldFileVersion.containsKey(fileName)) {
                if (!oldFileVersion.get(fileName).equals(newFileVersion.get(fileName))) {
                    FileUtils.writeGitletObjectsFile(stagedFiles.get(newFileVersion.get(fileName)));
                }
            } else  {
                FileUtils.writeGitletObjectsFile(stagedFiles.get(newFileVersion.get(fileName)));
            }
        }
    }

    public static boolean isTrackedByCommit(String commitId, String fileName) {
        Commit commit = readCommit(commitId);
        return isTrackedByCommit(commit, fileName);
    }

    public static boolean isTrackedByCommit(Commit commit, String fileName) {
        assert commit != null && fileName != null;
        return commit.getFileVersionMap().containsKey(fileName);
    }

    public static boolean isSameCommit(Commit commit1, Commit commit2) {
        assert commit1 != null && commit2 != null;
        return getCommitId(commit1).equals(getCommitId(commit2));
    }

    /**
     * trace back to the initial commit
     */
    public static List<Commit> commitTraceBack(Commit currentCommit) {
        List<Commit> commitList = new LinkedList<>();
        Commit commitPtr = currentCommit;
        while (commitPtr != null) {
            commitList.add(commitPtr);
            commitPtr = readCommit(commitPtr.getParentId());
        }
        return commitList;
    }

    /**
     * get the split point of two branches
     * @return if the two list has same length and has same commit list, then return null
     */
    public static Commit getSplitCommit(String branchName1, String branchName2) {
        String branch1CommitId = BranchUtils.getCommitId(branchName1);
        String branch2CommitId = BranchUtils.getCommitId(branchName2);
        Commit commit1 = readCommit(branch1CommitId);
        Commit commit2 = readCommit(branch2CommitId);
        List<Commit> branch1Traced = commitTraceBack(commit1);
        List<Commit> branch2Traced = commitTraceBack(commit2);
        Collections.reverse(branch1Traced); // bug : the list should be  old commit --> new commit !
        Collections.reverse(branch2Traced);
        int minLength = Math.min(branch1Traced.size(), branch2Traced.size());
        for (int i = 0; i < minLength; ++i) {
            // the front commit of the first different commit is the split point
            if (!isSameCommit(branch1Traced.get(i), branch2Traced.get(i))) {
                return branch1Traced.get(i - 1);
            }
        }
        // if the two list has same length and has same commit list, then return null
        if (branch1Traced.size() == branch2Traced.size()) {
            return null;
        }
        // in minLength range, the two list has same commit, then the end elem of shorter list will be return
        return branch1Traced.size() < branch2Traced.size() ?
                branch1Traced.get(branch1Traced.size() - 1) : branch2Traced.get(branch1Traced.size() - 1);
    }

    /**
     * return if two commit has a same file version, given the file name
     * @return if one of the commits doesn't contain the file, return null. else return true or false
     */
    public static Boolean hasSameFileVersion(String fileName, Commit commit1, Commit commit2) {
        assert commit1 != null && commit2 != null && fileName != null;
        HashMap<String, String> fileVersionMap1 = commit1.getFileVersionMap();
        HashMap<String, String> fileVersionMap2 = commit2.getFileVersionMap();
        if (!fileVersionMap1.containsKey(fileName) || !fileVersionMap2.containsKey(fileName)) {
            return null;
        }
        return fileVersionMap1.get(fileName).equals(fileVersionMap2.get(fileName));
    }

    /**
     * check consistency of a file with fileName.
     * what is consistency ? it means two commits:
     * 1. both have the file or both don't have the file,
     * 2. if both have the file, it must have the same file version
     */
    public static boolean isConsistent(String fileName, Commit commit1, Commit commit2) {
        assert commit1 != null && commit2 != null && fileName != null;
        HashMap<String, String> fileVersionMap1 = commit1.getFileVersionMap();
        HashMap<String, String> fileVersionMap2 = commit2.getFileVersionMap();
        boolean existInCommit1 = fileVersionMap1.containsKey(fileName);
        boolean existInCommit2 = fileVersionMap2.containsKey(fileName);
        if (!existInCommit1 && !existInCommit2) {
            return true;
        }
        if (!existInCommit1 || !existInCommit2) {
            return false;
        }
        Boolean sameContent = hasSameFileVersion(fileName, commit1, commit2);
        assert sameContent != null;
        return sameContent;
    }
}
