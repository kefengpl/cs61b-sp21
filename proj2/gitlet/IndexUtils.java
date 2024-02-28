package gitlet;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import static gitlet.GitletConstants.*;
import static gitlet.Utils.*;

/**
 * @Author 3590
 * @Date 2024/2/24 16:07
 * @Description the right usage of this class is: change anything in memory, and finally save your change.
 * yes, every command changes index MUST call the method saveIndex() to save their change permanently
 */
public class IndexUtils {
    /** read the INDEX file(which stores a map, file name --> version), it represents next commits' name --> version map,
     * which means just after one commit, the indexMap equals to commit fileVersionMap */
    public static HashMap<String, String> indexMap;
    /** staged files, which stages file id(sha1) --> file contents, stage or unstage file */
    public static HashMap<String, String> stagedFileContents;

    static {
        if (Repository.isInitialized()) {
            indexMap = readIndex();
            stagedFileContents = readStagedContents();
        }
    }

    /***
     * this function will write indexMap & stagedFileContents to INDEX_FILE & STAGED_FILE
     * every change to index must be saved
     * @note you must storage stagedFileContents to STAGED_FILE and clear it after one commit!
     */
    public static void saveIndex() {
        Utils.writeObject(INDEX_FILE, indexMap);
        Utils.writeObject(STAGED_FILE, stagedFileContents);
    }

    /**
     * stages a file (note: if file wrong, it will throw exception) in indexMap and stagedFileContents
     * @note this function will NOT save anything to disk, just keep them in memory
     */
    public static void stageFile(String fileName) {
        // update: to save the space, we can just save sha1 in index map, and not really create the file object,
        // but store the file in a map(staged) and save it. at commit, we create the object
        String fileContents = readContentsAsString(join(CWD, fileName));
        String fileSHA1 = sha1(fileContents);
        indexMap.put(fileName, fileSHA1);
        stagedFileContents.put(fileSHA1, fileContents); // save the file content in memory not disk
    }

    /***
     * unstage a file in memory
     * @note this function will NOT save anything to disk, just keep them in memory
     * @note there maybe redundant entry in stagedFileContents, but it will finally be cleared once commit.
     */
    public static void unstageFile(String fileName) {
        String fileSHA1 = sha1(indexMap.get(fileName));
        stagedFileContents.remove(fileSHA1);
        indexMap.remove(fileName);
    }

    public static HashMap<String, String> readIndex() {
        return hashMapRead(INDEX_FILE);
    }

    public static HashMap<String, String> readStagedContents() {
        return hashMapRead(STAGED_FILE);
    }

    /***
     * helper function for readIndex and readStagedContents
     */
    public static HashMap<String, String> hashMapRead(File file) {
        if (file.length() == 0) {
            return new HashMap<>();
        }
        // bug: you have to check if the index file is empty to avoid EOF exception
        HashMap<String, String> hashMap = Utils.readObject(file, HashMap.class);
        return hashMap != null ? hashMap : new HashMap<>();
    }

    /***
     * get staged files for git status.
     * it compares indexMap and commit.fileVersionMap
     * @param commit current commit to compare to
     */
    public static List<String> getStagedFiles(Commit commit) {
        HashMap<String, String> fileVersionMap = commit.getFileVersionMap();
        List<String> result = new LinkedList<>();
        for (String fileName : indexMap.keySet()) {
            if (fileVersionMap.containsKey(fileName)) {
                if (!fileVersionMap.get(fileName).equals(indexMap.get(fileName))) {
                    result.add(fileName);
                }
            } else {
                result.add(fileName);
            }
        }
        result.sort(String::compareTo);
        return result;
    }

    /***
     * the so-called removed files, is unstaged from indexMap.
     * it also means the file in commit.getVersionMap() but not in indexMap
     * @param commit current commit to compare to
     */
    public static List<String> getRemovedFiles(Commit commit) {
        HashMap<String, String> fileVersionMap = commit.getFileVersionMap();
        List<String> result = new LinkedList<>();
        for (String fileName : fileVersionMap.keySet()) {
            if (!indexMap.containsKey(fileName)) {
                result.add(fileName);
            }
        }
        result.sort(String::compareTo);
        return result;
    }

    /**
     * a staged file is: a file in indexMap but not in commit fileVersionMap;
     * or a file in indexMap and in commit fileVersionMap but has different version.
     * these staged (file --> version) in indexMap will finally be created in .gitlet/objects
     */
    public static boolean isStaged(String fileName, Commit commit) {
        assert fileName != null && commit != null;
        HashMap<String, String> fileVersionMap = commit.getFileVersionMap();
        return (indexMap.containsKey(fileName) && !fileVersionMap.containsKey(fileName))
                || (indexMap.containsKey(fileName) && fileVersionMap.containsKey(fileName)
                    && !fileVersionMap.get(fileName).equals(indexMap.get(fileName)));
    }

    /**
     * these removal files will finally be drop in next commit fileVersionMap.
     */
    public static boolean isRemoval(String fileName, Commit commit) {
        assert fileName != null && commit != null;
        return commit.getFileVersionMap().containsKey(fileName) && !indexMap.containsKey(fileName);
    }

    /**
     * ("Untracked Files") is for files present in the working directory but neither staged for addition nor tracked.
     * @note may be this method should be implemented in CommitUtils
     */
    public static List<String> getUntrackedFiles(Commit commit) {
        List<String> CWDFileNames = plainFilenamesIn(CWD);
        List<String> result = new LinkedList<>();
        assert CWDFileNames != null;
        for (String fileName : CWDFileNames) {
            if (!isStaged(fileName, commit) && !CommitUtils.isTrackedByCommit(commit, fileName)) {
                result.add(fileName);
            }
        }
        return result;
    }

    /**
     * "modified but not staged"
     * Staged for addition, but with different contents than in the working directory; (modified) or
     * Tracked in the current commit, changed in the working directory, but not staged; (deleted) or
     * Staged for addition, but deleted in the working directory; or
     * Not staged for removal, but tracked in the current commit and deleted from the working directory.
     * @return modifiedNotStagedForCommit file name list
     */
    public static List<StringBuffer> modifiedNotStagedForCommit(Commit commit) {
        List<String> CWDFileNames = plainFilenamesIn(CWD);
        List<StringBuffer> result = new LinkedList<>();
        assert CWDFileNames != null;
        for (String fileName : CWDFileNames) {
            boolean fileIsStaged = isStaged(fileName, commit);
            boolean fileIsTracked = CommitUtils.isTrackedByCommit(commit, fileName);
            if ((fileIsStaged && !FileUtils.hasSameSHA1(fileName, indexMap.get(fileName))) ||
                    (fileIsTracked && !FileUtils.hasSameSHA1(fileName, commit.getFileVersionMap().get(fileName)) && !fileIsStaged)) {
                result.add(new StringBuffer(fileName));
            }
        }
        return result;
    }

    /**
     * Staged for addition, but deleted in the working directory; or
     * Not staged for removal, but tracked in the current commit and deleted from the working directory.
     * @return deletedNotStagedForCommit file name list
     */
    public static List<StringBuffer> deletedNotStagedForCommit(Commit commit) {
        List<String> CWDFileNames = plainFilenamesIn(CWD);
        assert CWDFileNames != null;
        List<StringBuffer> result = new LinkedList<>();
        List<String> stagedFiles = getStagedFiles(commit);
        for (String fileName : stagedFiles) {
            if (!CWDFileNames.contains(fileName)) {
                result.add(new StringBuffer(fileName));
            }
        }
        HashMap<String, String> fileVersionMap = commit.getFileVersionMap();
        for (String fileName : fileVersionMap.keySet()) {
            if (!CWDFileNames.contains(fileName) && !isRemoval(fileName, commit)) {
                result.add(new StringBuffer(fileName));
            }
        }
        return result;
    }
}
