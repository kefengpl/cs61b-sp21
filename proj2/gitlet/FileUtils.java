package gitlet;

import static gitlet.GitletConstants.*;
import static gitlet.Utils.*;

/**
 * @Author 3590
 * @Date 2024/2/24 14:55
 * @Description
 */
public class FileUtils {
    /***
     * judge if the file has the same sha-1 with targetSHA1
     * (which also means they have the same contents)
     */
    public static boolean hasSameSHA1(String fileName, String targetSHA1) {
        return getFileContentSHA1(fileName).equals(targetSHA1);
    }

    public static String getFileContent(String fileSHA1) {
        return readContentsAsString(join(OBJECTS_DIR, fileSHA1));
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
}
