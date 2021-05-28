package info.skyblond.nekohit.neo.helper;

import io.neow3j.devpack.StorageContext;
import io.neow3j.devpack.annotations.Syscall;
import static io.neow3j.script.InteropService.SYSTEM_STORAGE_PUT;

public class StorageHelper {
    @Syscall(SYSTEM_STORAGE_PUT)
    public static native void put(StorageContext context, String key, long value);

    @Syscall(SYSTEM_STORAGE_PUT)
    public static native void put(StorageContext context, byte[] key, long value);
}
