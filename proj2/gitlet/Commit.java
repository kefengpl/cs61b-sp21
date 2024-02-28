package gitlet;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Interpret transient:
 * when you write an object to file, it will write itself and all members it references to
 * Don’t use Java pointers to refer to commits and blobs in your runtime objects, but instead use SHA-1 hash strings
 * Maintain a runtime map(never write to file) between these SHA-1 strings and the runtime objects they refer to.
 * (I think this runtime map should be stored in Repository, not in this class)
 * @description this class is just a JavaBean which stores critical information. Not for doing anything.
 * this class will be serialized to a file in [commits] folder in [.gitlet]
 */
public class Commit implements Serializable {
    /** The message of this Commit. */
    private String message;
    /** the commit time stamp */
    private Date commitTime;

    /** parentSHA1 value */
    private String parentId;

    /** second Parent */
    private String secondParentId;

    /** store flat file names and its version(represented by SHA-1) */
    private HashMap<String, String> fileVersionMap;

    /***
     * fileVersionMap will never be null.
     */
    public Commit() {
        fileVersionMap = new HashMap<>();
    }

    public String getMessage() {
        return message;
    }

    public Date getCommitTime() {
        return commitTime;
    }

    public String getParentId() {
        return parentId;
    }

    public String getSecondParentId() {
        return secondParentId;
    }

    public HashMap<String, String> getFileVersionMap() {
        return fileVersionMap;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setCommitTime(Date commitTime) {
        this.commitTime = commitTime;
    }

    public void setParentId(String ParentId) {
        this.parentId = ParentId;
    }

    public void setSecondParentId(String secondParentId) {
        this.secondParentId = secondParentId;
    }

    public void setFileVersionMap(HashMap<String, String> fileVersionMap) {
        this.fileVersionMap = fileVersionMap;
    }

    /***
     * print key info for log command
     */
    public void printCommitInfo() {
        SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM d HH:mm:ss yyyy Z", Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("GMT-8"));
        System.out.println("===");
        System.out.println("commit " + CommitUtils.getCommitId(this));
        System.out.println("Date: " + sdf.format(this.commitTime));
        System.out.println(this.message);
        System.out.println();
    }
}
