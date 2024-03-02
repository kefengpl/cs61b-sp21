package gitlet;

import java.util.Arrays;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static gitlet.GitletConstants.*;

/**
 * @description Driver class for Gitlet, a subset of the Git version-control system.
 * hint: checkout command will also restore the index region(so-called "work tree clean")
 * checkout just remove the HEAD pointer to a commit (maybe in other branch)
 *
 * use a HEAD FILE to store e.g. HEAD --> master information
 * use a branches directory to store different branch and it's pointer to commit
 * for example: master branch uses a file named [master] and store [commit id(SHA-1)] in it.
 *
 * hint: runnable has no args and no return! you can just regard runnable as a normal class
 */
public class Main {

    /**
     * @note
     * other error to be supplied
     * 1. inputs a command with the wrong number or format of operands --> Incorrect operands.
     * 2. command must with .gitlet folder created but hasn't been created --> Not in an initialized Gitlet directory.
     * */
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Please enter a command.");
            return;
        }
        String firstArg = args[0];
        String[] restArgs = Arrays.copyOfRange(args, 1, args.length);
        switch(firstArg) {
            case "init":
                if (restArgs.length == 0) {
                    Repository.init();
                } else {
                    System.out.println(INCORRECT_OPERANDS_WARNING);
                }
                break;
            case "add":
                commandRunner(restArgs.length == 1, Repository::add, restArgs[0]);
                break;
            case "commit":
                commandRunner(restArgs.length == 1, Repository::commit, restArgs[0]);
                break;
            case "rm":
                commandRunner(restArgs.length == 1, Repository::rm, restArgs[0]);
                break;
            case "log":
                commandRunner(restArgs.length == 0, Repository::log);
                break;
            case "global-log":
                commandRunner(restArgs.length == 0, Repository::globalLog);
                break;
            case "checkout":
                commandRunner(restArgs.length >= 1 && restArgs.length <= 3, Repository::checkout, restArgs);
                break;
            case "branch":
                commandRunner(restArgs.length == 1, Repository::branch, restArgs[0]);
                break;
            case "find":
                commandRunner(restArgs.length == 1, Repository::find, restArgs[0]);
                break;
            case "status":
                commandRunner(restArgs.length == 0, Repository::status);
                break;
            case "rm-branch":
                commandRunner(restArgs.length == 1, Repository::removeBranch, restArgs[0]);
                break;
            case "reset":
                commandRunner(restArgs.length == 1, Repository::reset, restArgs[0]);
                break;
            case "merge":
                commandRunner(restArgs.length == 1, Repository::merge, restArgs[0]);
                break;
            case "add-remote":
                commandRunner(restArgs.length == 2, RemoteUtils::addRemote, restArgs[0], restArgs[1]);
                break;
            case "rm-remote":
                commandRunner(restArgs.length == 1, RemoteUtils::removeRemote, restArgs[0]);
                break;
            case "push":
                commandRunner(restArgs.length == 2, RemoteUtils::push, restArgs[0], restArgs[1]);
                break;
            case "fetch":
                commandRunner(restArgs.length == 2, RemoteUtils::fetch, restArgs[0], restArgs[1]);
                break;
            case "pull":
                commandRunner(restArgs.length == 2, RemoteUtils::pull, restArgs[0], restArgs[1]);
                break;
            case "test":
                break;
            default:
                System.out.println("No command with that name exists.");
        }
    }

    /***
     * check one command whether after init() and check arg numbers at the same time
     * @param argsNumberCheck we suggest you adding logic expression like: restArgs.length != 0
     * @param function as a Function interface for lambda
     */
    private static <T> void commandRunner(boolean argsNumberCheck, Consumer<T> function, T args) {
        if (!Repository.isInitialized()) {
            System.out.println(UNINITIALIZED_WARNING);
            return;
        }
        if (!argsNumberCheck) {
            System.out.println(INCORRECT_OPERANDS_WARNING);
            return;
        }
        function.accept(args);
    }

    /***
     * check one command whether after init() and check arg numbers at the same time
     * @param argsNumberCheck we suggest you adding logic expression like: restArgs.length != 0
     * @param function as a Function interface for lambda
     */
    private static <T1, T2> void commandRunner(boolean argsNumberCheck, BiConsumer<T1, T2> function, T1 args1, T2 args2) {
        if (!Repository.isInitialized()) {
            System.out.println(UNINITIALIZED_WARNING);
            return;
        }
        if (!argsNumberCheck) {
            System.out.println(INCORRECT_OPERANDS_WARNING);
            return;
        }
        function.accept(args1, args2);
    }

    /***
     * similar to upper function with no args.
     * @param argsNumberCheck we suggest you adding logic expression like: restArgs.length != 0
     * @param function as a Function interface for lambda
     */
    private static void commandRunner(boolean argsNumberCheck, Runnable function) {
        if (!Repository.isInitialized()) {
            System.out.println(UNINITIALIZED_WARNING);
            return;
        }
        if (!argsNumberCheck) {
            System.out.println(INCORRECT_OPERANDS_WARNING);
            return;
        }
        function.run();
    }
}
