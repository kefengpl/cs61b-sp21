package gitlet;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import static gitlet.GitletConstants.*;
import static gitlet.Utils.*;

/**
 * @Author 3590
 * @Date 2024/2/24 15:09
 * @Description
 */
public class BranchUtils {
    /**
     * get the file object of origin(folder) with branch name example origin/master
     */
    public static File getRemoteBranchFolder(String branchName) {
        assert branchName != null && branchName.contains("/");
        String[] split = branchName.split("/");
        return join(BRANCHES_DIR, split[0]);
    }

    /**
     * get the file object of branch(file) with branch name example origin/master
     */
    public static File getRemoteBranchFile(String branchName) {
        assert branchName != null && branchName.contains("/");
        String[] split = branchName.split("/");
        return join(BRANCHES_DIR, split[0], split[1]);
    }

    public static String getCommitId(String branchName) {
        if (branchName.contains("/")) { // handle case: origin/master
            File remoteBranchFile = getRemoteBranchFile(branchName);
            return readContentsAsString(remoteBranchFile);
        }
        return readContentsAsString(join(BRANCHES_DIR, branchName));
    }

    /**
     * set current branch points to new commit id
     */
    public static void saveCommitId(String branchName, String commitId) {
        if (branchName.contains("/")) { // handle case: origin/master
            String[] split = branchName.split("/");
            File folder = join(BRANCHES_DIR, split[0]);
            if (!folder.exists()) {
                folder.mkdir();
            }
            Utils.writeContents(join(folder, split[1]), commitId);
            return;
        }
        Utils.writeContents(join(BRANCHES_DIR, branchName), commitId);
    }

    /**
     * it will NOT check the existence of branch, directly remove the relevant branch file.
     * @note it does not remove any commit.
     */
    public static boolean removeBranch(String branchName) {
        if (branchName.contains("/")) {
            return getRemoteBranchFile(branchName).delete();
        }
        return join(BRANCHES_DIR, branchName).delete();
    }

    /***
     * @return branchNameList with dictionary order
     */
    public static List<String> getAllBranchNames() {
        List<String> branchNameList = plainFilenamesIn(BRANCHES_DIR);
        assert branchNameList != null;
        branchNameList = new LinkedList<>(branchNameList); // allocate new memory space to modify this variable
        File[] remoteFolders = BRANCHES_DIR.listFiles(File::isDirectory);
        if (remoteFolders != null) {
            for (File remoteFolder : remoteFolders) {
                List<String> remoteBranches = plainFilenamesIn(remoteFolder);
                assert remoteBranches != null;
                for (String remoteName : remoteBranches) {
                    branchNameList.add(remoteFolder.getName() + "/" + remoteName);
                }
            }
        }
        branchNameList.sort(String::compareTo);
        return branchNameList;
    }

    /**
     * query if one branch exists in .gitlet/branches
     * @param branchName must be not null. this function will assert it.
     */
    public static boolean branchExists(String branchName) {
        if (branchName.contains("/")) {
            return getRemoteBranchFile(branchName).exists();
        }
        List<String> branchNameList = getAllBranchNames();
        return branchNameList.contains(branchName);
    }

}
