package com.microsoft.applicationinsights;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

class ByteBufferOutputStream extends OutputStream {

    private final AppInsightsByteBufferPool byteBufferPool;

    private final List<ByteBuffer> byteBuffers = new ArrayList<>();

    private ByteBuffer current;

    ByteBufferOutputStream(AppInsightsByteBufferPool byteBufferPool) {
        this.byteBufferPool = byteBufferPool;
        current = byteBufferPool.remove();
        byteBuffers.add(current);
    }

    @Override
    public void write(int b) throws IOException {
        ensureSomeCapacity();
        current.put((byte) b);
    }

    @Override
    public void write(byte[] bytes, int off, int len) throws IOException {
        ensureSomeCapacity();
        int numBytesWritten = Math.min(current.remaining(), len);
        current.put(bytes, off, numBytesWritten);
        if (numBytesWritten < len) {
            write(bytes, off + numBytesWritten, len - numBytesWritten);
        }
    }

    void ensureSomeCapacity() {
        if (current.remaining() > 0) {
            return;
        }
        current = byteBufferPool.remove();
        byteBuffers.add(current);
    }

    List<ByteBuffer> getByteBuffers() {
        return byteBuffers;
    }
}
