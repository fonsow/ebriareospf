package MiscTests;

public class TryCatchFinallyContinueTest {
    public static void main(String[] args) {
        for (int i = 0; i < 3; i++) {
            System.out.println();

            try {
                System.out.println("This is a try block");

                if (i == 1) {
                    throw new Exception();
                }
            } catch (Exception e) {
                System.out.println("Caught exception. Continuing...");
                continue;
            } finally {
                System.out.println("The 'finally' block is executed");
            }

            System.out.println("This is written the first and third times, but not the second time");
        }
    }
}
