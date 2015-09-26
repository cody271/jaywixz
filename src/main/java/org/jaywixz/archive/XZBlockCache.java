package org.jaywixz.archive;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import org.tukaani.xz.SeekableFileInputStream;
import org.tukaani.xz.SeekableInputStream;
import org.tukaani.xz.SeekableXZInputStream;


public class XZBlockCache {

    private static final int MAX_BLOCKS = 128;

    private static final Logger log = Logger.getLogger(XZBlockCache.class);

    private Map<Integer, byte[]> blocks = new LinkedHashMap<Integer, byte[]>(MAX_BLOCKS, .75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Integer, byte[]> eldest) {
            boolean remove = size() >= MAX_BLOCKS;
            if (remove) {
                log.debug("removeEldestEntry() " + eldest.getKey());
            }
            return remove;
        }
    };
    private byte[] currentBlock;
    private SeekableXZInputStream is;

    public XZBlockCache(String archivePath) throws IOException {
        is = new SeekableXZInputStream(new SeekableFileInputStream(archivePath));
        log.debug(is);
    }

    public SeekableInputStream stream() {
        return is;
    }

    private long loadBlock(long pos) throws IOException {
        int blockNo = is.getBlockNumber(pos);
        long blockPos = is.getBlockPos(blockNo);
        if (!blocks.keySet().contains(blockNo)) {
            int blockSize = (int) is.getBlockSize(blockNo);
            byte[] block = new byte[blockSize];
            is.seek(blockPos);
            int res = is.read(block, 0, blockSize);
            if (res != blockSize) throw new IOException();
            blocks.put(blockNo, block);
            log.debug("loadBlock() " + blockNo + " " + blockSize);
        }
        currentBlock = blocks.get(blockNo);
        return blockPos;
    }

    public void read(long pos, byte[] buffer, int offset, int length, boolean cache) throws IOException {
        if (cache) {
            int srcOffset = (int) (pos - loadBlock(pos));
            if (currentBlock.length >= srcOffset + length) {
                System.arraycopy(currentBlock, srcOffset, buffer, offset, length);
            } else {
                read(pos, buffer, offset, length, false);
            }
        } else {
            is.seek(pos);
            int res = is.read(buffer, offset, length);
            if (res != length) throw new IOException();
            log.debug("read() " + is.getBlockNumber(pos) + " " + length);
        }
    }

    public void read(long pos, byte[] buffer, int offset, int length) throws IOException {
        read(pos, buffer, offset, length, true);
    }
}
