package com.nekohit.neo.helper;

import io.neow3j.devpack.ByteString;
import io.neow3j.devpack.StorageContext;
import io.neow3j.devpack.annotations.Instruction;

import static io.neow3j.script.InteropService.SYSTEM_STORAGE_PUT;

public class StorageHelper {
    @Instruction(interopService = SYSTEM_STORAGE_PUT)
    public static native void put(StorageContext context, String key, long value);

    @Instruction(interopService = SYSTEM_STORAGE_PUT)
    public static native void put(StorageContext context, ByteString key, long value);
}
