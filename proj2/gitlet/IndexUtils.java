package gitlet;

import java.io.File;
import java.util.HashMap;
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
}
