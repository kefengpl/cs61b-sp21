/** Class that prints the Collatz sequence starting from a given number.
 *  @author YOUR NAME HERE
 */
public class Collatz {

    /** Get the next number of Collatz
     * if n is odd, then return 3 * n + 1
     * if n is even, then return n / 2
     * */
    public static int nextNumber(int n) {
        if (n == 1) {
            return 1;
        }
        if (n % 2 == 0) {
            return n / 2;
        } else {
            return 3 * n + 1;
        }
    }

    public static void main(String[] args) {
        int n = 5;
        System.out.print(n + " ");
        while (n != 1) {
            n = nextNumber(n);
            System.out.print(n + " ");
        }
        System.out.println();
    }
}

