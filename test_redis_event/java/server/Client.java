package com.apusic.cache.nioperformancetest.server;

import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.Queue;

public class Client {

    final Queue<CommandPackage> inPackages;

    // final Queue<CommandPackage> outPackage;

    public Client() {
        this.inPackages = new LinkedList<CommandPackage>();
        // this.outPackage = new LinkedList<CommandPackage>();
    }

    boolean readingHead = true;
    byte[] headByte = new byte[4];
    int headPos = 0;

    CommandPackage pendingPackage = null;

    public CommandPackage getOutputPackage() {
        return this.inPackages.poll();
    }

    public boolean isWritable() {
        return this.inPackages.size() > 0 ? true : false;
    }

    public void putBytes(ByteBuffer buffer) {
        if (buffer.hasRemaining() == false) {
            return;
        }

        while (buffer.hasRemaining()) {

            if (pendingPackage == null) {
                pendingPackage = new CommandPackage();
            }

            if (readingHead) {
                int need = 4 - headPos;
                if (need <= buffer.remaining()) {
                    for (int i = 0; i < need; i++) {
                        headByte[headPos + i] = buffer.get();
                    }
                    this.pendingPackage.length = ((headByte[0] & 0xFF) << 24) | ((headByte[1] & 0xFF) << 16)
                                                 | ((headByte[2] & 0xFF) << 8) | (headByte[3] & 0xFF);
                    
                    this.pendingPackage.data = new byte[pendingPackage.length];
                    this.pendingPackage.lengthBytes[0] = headByte[0];
                    this.pendingPackage.lengthBytes[1] = headByte[1];
                    this.pendingPackage.lengthBytes[2] = headByte[2];
                    this.pendingPackage.lengthBytes[3] = headByte[3];
                    readingHead = false;
                    headPos = 0;
                } else {
                    int remain = buffer.remaining();
                    for (int i = 0; i < remain; i++) {
                        headByte[headPos + i] = buffer.get();
                    }
                    headPos += remain;
                    continue;
                }
            }

            if (readingHead == false) {
                int remain = buffer.remaining();
                int need = pendingPackage.data.length - pendingPackage.writePos;
                int pn = need < remain ? need : remain;
                buffer.get(this.pendingPackage.data, this.pendingPackage.writePos, pn);
                this.pendingPackage.writePos += pn;

                if (this.pendingPackage.writePos == this.pendingPackage.data.length) {
                    this.inPackages.add(pendingPackage);
                    pendingPackage = null;
                    this.readingHead = true;
                }
            }
        }

    }

}
