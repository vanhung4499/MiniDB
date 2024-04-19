package com.hnv99.minidb.backend.utils;

public class Types {
    public static long addressToUid(int page, short offset) {
        return (long)page << 32 | (long)offset;
    }
}
