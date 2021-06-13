package info.skyblond.nekohit.neo.helper;

public class Utils {
    /**
     * Similar to kotlin's reqiure function. If condition is false, then exception is thrown
     * @param condition the condition required to check
     * @param message if condition is false, the message for exception
     * @throws Exception
     */
    public static void require(boolean condition, String message) throws Exception {
        if (!condition) {
            throw new Exception(message);
        }
    }
}
