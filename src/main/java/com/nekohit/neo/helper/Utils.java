package com.nekohit.neo.helper;

import io.neow3j.devpack.ByteString;
import io.neow3j.devpack.annotations.Instruction;
import io.neow3j.script.OpCode;
import io.neow3j.types.StackItemType;

public class Utils {
    @Instruction(opcode = OpCode.CONVERT, operand = StackItemType.BYTE_STRING_CODE)
    public static native ByteString intToByteString(int i);

    public static ByteString paddingByteString(ByteString b, int l) throws Exception {
        require(b.length() <= l, "Max length exceeded.");
        if (b.length() < l) {
            byte[] buffer = new byte[l - b.length()];
            b = b.concat(buffer);
        }
//        require(b.length() == l, "assert failed!");
        return b;
    }

    /**
     * Similar to kotlin's require function. If condition is false, then exception is thrown
     *
     * @param condition the condition required to check
     * @param message   if condition is false, the message for exception
     * @throws Exception if the condition is failed
     */
    public static void require(boolean condition, String message) throws Exception {
        if (!condition) {
            throw new Exception(message);
        }
    }
}
