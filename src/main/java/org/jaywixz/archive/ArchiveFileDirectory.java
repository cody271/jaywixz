package org.jaywixz.archive;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.lucene.store.BaseDirectory;
import org.apache.lucene.store.IOContext;
import org.apache.lucene.store.IndexInput;
import org.apache.lucene.store.IndexOutput;
import org.apache.lucene.store.NoLockFactory;


public class ArchiveFileDirectory extends BaseDirectory {

    private XZBlockCache xz;
    private Map<String, List<Long>> footer;

    public ArchiveFileDirectory(XZBlockCache xz, Map<String, List<Long>> footer) throws IOException {
        super(NoLockFactory.INSTANCE);
        this.xz = xz;
        this.footer = footer;
        this.isOpen = true;
    }

    @Override
    public String[] listAll() throws IOException {
        return footer.keySet().toArray(new String[0]);
    }

    @Override
    public IndexInput openInput(String name, IOContext arg1) throws IOException {
        List<Long> offset = footer.get(name);
        return new ArchiveFileIndexInput(xz, name,
                offset.get(0).longValue(),
                offset.get(1).longValue());
    }

    @Override
    public long fileLength(String arg0) throws IOException {
        return footer.get(arg0).get(1).longValue();
    }

    private static final class ArchiveFileIndexInput extends IndexInput {

        private XZBlockCache xz;
        private long offset, size, pos;

        public ArchiveFileIndexInput(XZBlockCache xz, String name, long offset, long size) {
            super(name);
            this.xz = xz;
            this.offset = offset;
            this.size = size;
            this.pos = 0;
        }

        @Override
        public byte readByte() throws IOException {
            byte[] singleByte = new byte[1];
            this.readBytes(singleByte, 0, 1);
            return singleByte[0];
        }

        @Override
        public void readBytes(byte[] buffer, int arrayOffset, int length) throws IOException {
            xz.read(this.offset + this.pos, buffer, arrayOffset, length);
            this.pos += length;
        }

        @Override
        public void seek(long pos) throws IOException {
            this.pos = pos;
        }

        @Override
        public long getFilePointer() {
            return this.pos;
        }

        @Override
        public long length() {
            return this.size;
        }

        @Override
        public IndexInput slice(String arg0, long arg1, long arg2) throws IOException {
            throw new IOException();
        }

        @Override
        public void close() throws IOException {
        }
    }

    @Override
    public void close() throws IOException {
    }

    @Override
    public void sync(Collection<String> arg0) throws IOException {
    }

    @Override
    public IndexOutput createOutput(String arg0, IOContext arg1)
            throws IOException {
        throw new IOException();
    }

    @Override
    public void deleteFile(String arg0) throws IOException {
        throw new IOException();
    }

    @Override
    public void renameFile(String arg0, String arg1) throws IOException {
        throw new IOException();
    }
}
