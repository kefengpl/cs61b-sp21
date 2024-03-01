package gitlet;

import java.util.HashMap;
import java.util.List;

import static gitlet.GitletConstants.*;
import static gitlet.Utils.*;

/**
 * @Author 3590
 * @Date 2024/2/24 14:55
 * @Description
 */
public class FileUtils {
    /***
     * judge if the file in CWD has the same sha-1 with targetSHA1
     * (which also means they have the same contents)
     */
    public static boolean hasSameSHA1(String fileName, String targetSHA1) {
        return getFileContentSHA1(fileName).equals(targetSHA1);
    }

    /**
     * read contents of file of some version from .gitlet/objects
     */
    public static String getFileContent(String fileSHA1) {
        return readContentsAsString(join(OBJECTS_DIR, fileSHA1));
    }

    /**
     * read contents of file of the version of commit from .gitlet/objects
     */
    public static String getFileContent(String fileName, Commit commit) {
        assert fileName != null && commit != null;
        return getFileContent(commit.getFileVersionMap().get(fileName));
    }

    /***
     * @param fileName the name of the file which is to be save as an object in .gitlet/objects
     * @return sha1 of the file content
     */
    public static String createGitletObjectFile(String fileName) {
        return writeGitletObjectsFile(readContentsAsString(join(CWD, fileName)));
    }

    /***
     * @param content the string contents of the file
     * @return sha1 of the file content
     */
    public static String writeGitletObjectsFile(String content) {
        String fileObjectId = sha1(content);
        writeContents(join(OBJECTS_DIR, fileObjectId), content);
        return fileObjectId;
    }

    public static void writeCWDFile(String fileName, String content) {
        writeContents(join(CWD, fileName), content);
    }

    public static String getFileContentSHA1(String fileName) {
        return sha1(readContentsAsString(join(CWD, fileName)));
    }

    /**
     * restore all files tracked of one commit to work directory.
     * after the "untracked file" check, no file tracked by pre-commit will be deleted.
     * however, some files in CWD tracked by pre-commit and not tracked by after-commit will be deleted.
     * some files will be created, which is after-commit tracked files, but not in CWD.
     * @note you must do "untracked file" check before calling this function
     */
    public static void restoreCommitFiles(Commit commit) {
        HashMap<String, String> fileVersionMap = commit.getFileVersionMap();
        List<String> CWDFileNames = plainFilenamesIn(CWD);
        assert CWDFileNames != null;
        for (String CWDFileName : CWDFileNames) {
            // delete not tracked files in this commit
            if (!fileVersionMap.containsKey(CWDFileName)) {
                Utils.restrictedDelete(join(CWD, CWDFileName));
            }
        }
        // restore files to CWD
        for (String fileName : fileVersionMap.keySet()) {
            writeCWDFile(fileName, getFileContent(fileVersionMap.get(fileName)));
        }
    }

    /**
     * @param fileName the file name of some commit which will be restored to CWD or deleted in CWD
     */
    public static boolean isOverwritingOrDeletingCWDUntracked(String fileName, Commit currentCommit) {
        List<String> CWDFileNames = plainFilenamesIn(CWD);
        assert CWDFileNames != null && currentCommit != null;
        return !CommitUtils.isTrackedByCommit(currentCommit, fileName) && CWDFileNames.contains(fileName);
    }
}
