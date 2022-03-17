package com.nekohit.neo.helper;

import io.neow3j.devpack.ByteString;

public class Utils {
    public static ByteString intToPaddingByteString(int i, int l) {
        ByteString b = new ByteString(i);
        assert b.length() <= l : "Max length exceeded.";
        if (b.length() < l) {
            // note in the NeoVM, int are represented in SMALL endian
            // So number 1024 = 0x400, which is (low addr)[00][04](high addr)
            // To padding the zero, we want it to be 0x00...000400
            // which is [00][04] [00]...[00]
            //          +--b---+ +---zero--+
            b = b.concat(new byte[l - b.length()]);
        }
        return b;
    }
}
