package com.apusic.cache.nioperformancetest.server;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

public class RWProtocal implements TCPProtocal {

    final int bufSize;
    final ByteBuffer readBuf;
    final byte[] data;

    public RWProtocal(int bufSize) {
        this.bufSize = bufSize;
        this.readBuf = ByteBuffer.allocate(bufSize);
        this.data = new byte[bufSize];
        for (int i = 0; i < data.length; i++) {
            data[i] = (byte) (i % 128);
        }
    }

    @Override
    public void handleAccept(SelectionKey key) throws IOException {
        ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
        SocketChannel clientChannel = serverChannel.accept();

        clientChannel.socket().setTcpNoDelay(true);
        clientChannel.socket().setKeepAlive(true);
        clientChannel.configureBlocking(false);

        Client client = new Client();
        clientChannel.register(key.selector(), SelectionKey.OP_READ, client);
        System.out.println("Accepted. " + clientChannel.socket().toString());
    }

    @Override
    public void handleRead(SelectionKey key) throws IOException {
        SocketChannel clientChannel = (SocketChannel) key.channel();

        Client client = (Client) key.attachment();
        readBuf.clear();
        try {
            long readBytes = clientChannel.read(this.readBuf);
            if (readBytes == -1) {
                clientChannel.close();
            } else if (readBytes > 0) {
                this.readBuf.rewind();
                this.readBuf.limit((int) readBytes);
                client.putBytes(readBuf);
                if (client.isWritable()) {
                    key.interestOps(SelectionKey.OP_WRITE);
                }
            }
        } catch (IOException ex) {
            clientChannel.close();
        }

    }

    @Override
    public void handleWrite(SelectionKey key) throws IOException {

        SocketChannel clientChannel = (SocketChannel) key.channel();
        // ByteBuffer writeBuf = ByteBuffer.wrap(this.data);
        //
        // while (writeBuf.hasRemaining()) {
        // clientChannel.write(writeBuf);
        // }

        Client client = (Client) key.attachment();
        while (client.isWritable()) {
            CommandPackage pack = client.getOutputPackage();
            if (pack != null) {
                ByteBuffer writeBuf = ByteBuffer.allocate(pack.length + 4);
                writeBuf.put(pack.lengthBytes);
                writeBuf.put(pack.data);
                writeBuf.flip();
                while (writeBuf.hasRemaining()) {
                    try {
                        clientChannel.write(writeBuf);
                    } catch (IOException ex) {
                        clientChannel.close();
                        return ;
                    }
                }
            } else {
                break;
            }
        }
        key.interestOps(SelectionKey.OP_READ);
    }

}
