package com.nekohit.neo.helper;

import io.neow3j.devpack.annotations.Struct;

@Struct
public class Pair<T, U> {
    public final T first;
    public final U second;

    public Pair(T t, U u) {
        first = t;
        second = u;
    }
}
