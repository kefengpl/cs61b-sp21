package gitlet;

import java.io.File;
import java.util.List;

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

    /**
     * it will NOT check the existence of branch, directly remove the relevant branch file.
     * @note it does not remove any commit.
     */
    public static boolean removeBranch(String branchName) {
        return join(BRANCHES_DIR, branchName).delete();
    }

    /***
     * @return branchNameList with dictionary order
     */
    public static List<String> getAllBranchNames() {
        List<String> branchNameList = plainFilenamesIn(BRANCHES_DIR);
        assert branchNameList != null;
        branchNameList.sort(String::compareTo);
        return branchNameList;
    }

    /**
     * query if one branch exists in .gitlet/branches
     * @param branchName must be not null. this function will assert it.
     */
    public static boolean branchExists(String branchName) {
        List<String> branchNameList = getAllBranchNames();
        return branchNameList.contains(branchName);
    }

}
