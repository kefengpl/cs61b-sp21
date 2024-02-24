package gitlet;

import java.io.File;

import static gitlet.GitletConstants.*;
import static gitlet.Utils.*;

/**
 * @Author 3590
 * @Date 2024/2/24 15:09
 * @Description
 */
public class BranchUtils {
    public static String getCommitId(String branchName) {
        return readContentsAsString(join(BRANCHES_DIR, branchName));
    }

    /**
     * set current branch points to new commit id
     */
    public static void saveCommitId(String branchName, String commitId) {
        Utils.writeContents(join(BRANCHES_DIR, branchName), commitId);
    }

}
