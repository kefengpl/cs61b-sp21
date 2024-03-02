package gitlet;

import java.io.File;

import static gitlet.Utils.join;

/**
 * @Author 3590
 * @Date 2024/2/20 21:26
 * @Description
 */
public class GitletConstants {
    public static final String MASTER_BRANCH_NAME = "master";
    /** The current working directory. */
    public static final File CWD = new File(System.getProperty("user.dir"));
    /** The .gitlet directory. */
    public static final File GITLET_DIR = join(CWD, ".gitlet");
    /** the index file */
    public static final File INDEX_FILE = join(GITLET_DIR, "index");
    /** store a map, remote name --> location(path) */
    public static final File REMOTE_FILE = join(GITLET_DIR, "remote");
    /** every remote repository must have it's own HEAD & master branch */
    public static final File REMOTE_FILE_DIR = join(GITLET_DIR, "remotes");
    /** the HEAD file */
    public static final File HEAD_FILE = join(GITLET_DIR, "HEAD");
    /** the commits directory, store every commit */
    public static final File COMMITS_DIR = join(GITLET_DIR, "commits");
    /** the object directory, store explicit files in it */
    public static final File OBJECTS_DIR = join(GITLET_DIR, "objects");
    public static final File BRANCHES_DIR = join(GITLET_DIR, "branches");
    public static final File STAGED_FILE = join(GITLET_DIR, "staged-files");

    public static final String UNINITIALIZED_WARNING = "Not in an initialized Gitlet directory.";
    public static final String INCORRECT_OPERANDS_WARNING = "Incorrect operands.";

    public static final String MERGE_MODIFY_UNTRACKED_WARNING = "There is an untracked file in the way; delete it, or add and commit it first.";
}
