package com.nekohit.neo.contract;

import io.neow3j.devpack.annotations.Instruction;
import io.neow3j.script.OpCode;

public class Utils {
    @Instruction(opcode = OpCode.ABORT)
    public static native void abort();

    public static void require(boolean condition, String message) throws Exception {
        if (!condition) {
            throw new Exception(message);
        }
    }
}
