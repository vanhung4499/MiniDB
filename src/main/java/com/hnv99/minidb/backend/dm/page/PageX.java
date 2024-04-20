package com.hnv99.minidb.backend.dm.page;

import com.hnv99.minidb.backend.dm.cache.PageCache;
import com.hnv99.minidb.backend.utils.Parser;

import java.util.Arrays;

/**
 * PageX manages ordinary pages
 * Structure of ordinary pages
 * [FreeSpaceOffset] [Data]
 * FreeSpaceOffset: 2 bytes Offset from the beginning of free space
 */
public class PageX {

    private static final short OF_FREE = 0;
    private static final short OF_DATA = 2;
    public static final int MAX_FREE_SPACE = PageCache.PAGE_SIZE - OF_DATA;

    public static byte[] initRaw() {
        byte[] raw = new byte[PageCache.PAGE_SIZE];
        setFSO(raw, OF_DATA);
        return raw;
    }

    private static void setFSO(byte[] raw, short ofData) {
        System.arraycopy(Parser.short2Byte(ofData), 0, raw, OF_FREE, OF_DATA);
    }

    // Get the FSO of pg
    public static short getFSO(Page pg) {
        return getFSO(pg.getData());
    }

    private static short getFSO(byte[] raw) {
        return Parser.parseShort(Arrays.copyOfRange(raw, 0, 2));
    }

    // Insert raw into pg and return the insertion position
    public static short insert(Page pg, byte[] raw) {
        pg.setDirty(true);
        short offset = getFSO(pg.getData());
        System.arraycopy(raw, 0, pg.getData(), offset, raw.length);
        setFSO(pg.getData(), (short)(offset + raw.length));
        return offset;
    }

    // Get the size of free space on the page
    public static int getFreeSpace(Page pg) {
        return PageCache.PAGE_SIZE - (int)getFSO(pg.getData());
    }

    // Insert raw into the offset position of pg and set pg's offset to the larger offset
    public static void recoverInsert(Page pg, byte[] raw, short offset) {
        pg.setDirty(true);
        System.arraycopy(raw, 0, pg.getData(), offset, raw.length);

        short rawFSO = getFSO(pg.getData());
        if(rawFSO < offset + raw.length) {
            setFSO(pg.getData(), (short)(offset + raw.length));
        }
    }

    // Insert raw into the offset position of pg without updating the update
    public static void recoverUpdate(Page pg, byte[] raw, short offset) {
        pg.setDirty(true);
        System.arraycopy(raw, 0, pg.getData(), offset, raw.length);
    }
}
